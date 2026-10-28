package com.ashrafwebdev.sikkanam.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.AppDatabase
import com.ashrafwebdev.sikkanam.data.MastooratEffort
import com.ashrafwebdev.sikkanam.data.MastooratRules
import com.ashrafwebdev.sikkanam.data.MastooratTaleem
import com.ashrafwebdev.sikkanam.data.MastooratTrip
import com.ashrafwebdev.sikkanam.data.Person
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "sikkanam_reminders"
private const val MASTOORAT_TAG = "sikkanam_mastoorat_reminder"
private const val KIND_KEY = "kind"
private const val ID_KEY = "id"
private const val KIND_TALEEM = 1
private const val KIND_TRIP = 2
private const val MASTOORAT_NOTIFICATION_BASE = 1_600_000

/** The taleem reminder goes out at 08:00 on the day of the meeting. */
private val TALEEM_HOUR_MILLIS = TimeUnit.HOURS.toMillis(8)

/** The trip reminder goes out at 18:00 the day before the trip starts. */
private val TRIP_LEAD_MILLIS = TimeUnit.HOURS.toMillis(6)

private fun workName(kind: Int, id: Long) = "sikkanam_mastoorat_${kind}_$id"

private fun schedule(context: Context, kind: Int, id: Long, fireMillis: Long) {
    val workManager = WorkManager.getInstance(context)
    val now = System.currentTimeMillis()
    if (fireMillis <= now) {
        workManager.cancelUniqueWork(workName(kind, id))
        return
    }
    val request = OneTimeWorkRequestBuilder<MastooratReminderWorker>()
        .setInitialDelay(fireMillis - now, TimeUnit.MILLISECONDS)
        .setInputData(Data.Builder().putInt(KIND_KEY, kind).putLong(ID_KEY, id).build())
        .addTag(MASTOORAT_TAG)
        .build()
    workManager.enqueueUniqueWork(workName(kind, id), ExistingWorkPolicy.REPLACE, request)
}

/** Reminds the men of their parts on the morning of a taleem meeting. [dayMillis] is the start of that day. */
fun scheduleMastooratTaleemReminder(context: Context, taleemId: Long, dayMillis: Long) {
    schedule(context, KIND_TALEEM, taleemId, dayMillis + TALEEM_HOUR_MILLIS)
}

/** Reminds the mahram men the evening before a trip starts. */
fun scheduleMastooratTripReminder(context: Context, tripId: Long, startMillis: Long) {
    schedule(context, KIND_TRIP, tripId, startMillis - TRIP_LEAD_MILLIS)
}

fun cancelMastooratTaleemReminder(context: Context, taleemId: Long) {
    WorkManager.getInstance(context).cancelUniqueWork(workName(KIND_TALEEM, taleemId))
}

fun cancelMastooratTripReminder(context: Context, tripId: Long) {
    WorkManager.getInstance(context).cancelUniqueWork(workName(KIND_TRIP, tripId))
}

/** Cancels every Mastoorat reminder, for example before a backup is restored. */
fun cancelAllMastooratReminders(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(MASTOORAT_TAG)
}

/** Sends one Mastoorat reminder, if its taleem or trip still exists. */
class MastooratReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val id = inputData.getLong(ID_KEY, 0L)
        val kind = inputData.getInt(KIND_KEY, 0)
        val db = AppDatabase.create(context)
        try {
            val names = db.personDao().all().associateBy { it.id }
            val (title, text) = when (kind) {
                KIND_TALEEM -> {
                    val taleem = db.mastooratDao().taleemById(id) ?: return Result.success()
                    context.getString(R.string.mastoorat_reminder_taleem_title) to taleemText(context, taleem, names)
                }
                KIND_TRIP -> {
                    val trip = db.mastooratDao().tripById(id) ?: return Result.success()
                    val pairs = db.mastooratDao().pairsOfTrip(trip.id).size
                    context.getString(R.string.mastoorat_reminder_trip_title) to tripText(context, trip, pairs)
                }
                else -> return Result.success()
            }
            ensureChannel(context)
            notify(context, MASTOORAT_NOTIFICATION_BASE + kind * 100_000 + id.toInt(), title, text)
        } finally {
            db.close()
        }
        return Result.success()
    }

    private fun taleemText(context: Context, taleem: MastooratTaleem, names: Map<Long, Person>): String {
        fun name(id: Long) = names[id]?.name ?: context.getString(R.string.mastoorat_role_none)
        return context.getString(
            R.string.mastoorat_reminder_taleem_text,
            taleem.house,
            name(taleem.readerPersonId),
            name(taleem.receptionPersonId),
            name(taleem.sixPointsPersonId),
            name(taleem.tashkeelPersonId),
        )
    }

    private fun tripText(context: Context, trip: MastooratTrip, pairs: Int): String {
        val effort = context.getString(
            when (trip.effort) {
                MastooratEffort.DAYS_3 -> R.string.mastoorat_effort_3
                MastooratEffort.DAYS_10 -> R.string.mastoorat_effort_10
                MastooratEffort.DAYS_40 -> R.string.mastoorat_effort_40
                MastooratEffort.MONTHS_2_FOREIGN -> R.string.mastoorat_effort_2m
            },
        )
        return context.getString(R.string.mastoorat_reminder_trip_text, effort, trip.place, pairs, MastooratRules.MAX_PAIRS)
    }

    private fun notify(context: Context, notificationId: Int, title: String, text: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
