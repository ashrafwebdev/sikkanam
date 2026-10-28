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
import com.ashrafwebdev.sikkanam.ui.dateLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "sikkanam_reminders"
private const val TRAVEL_TAG = "sikkanam_travel_reminder"
private const val TRAVEL_BOOKING_KEY = "booking_id"
private const val TRAVEL_NOTIFICATION_BASE = 1_500_000

/** How long before departure the travel reminder is sent. */
private val TRAVEL_LEAD_MILLIS = TimeUnit.HOURS.toMillis(12)

private fun travelWorkName(bookingId: Long) = "sikkanam_travel_$bookingId"

/**
 * Schedules the reminder for one booking, 12 hours before departure (or straight away if that has
 * already passed). A departure in the past gets no reminder. Scheduling again replaces the old one.
 */
fun scheduleTravelReminder(context: Context, bookingId: Long, departMillis: Long) {
    val workManager = WorkManager.getInstance(context)
    val now = System.currentTimeMillis()
    if (departMillis <= now) {
        workManager.cancelUniqueWork(travelWorkName(bookingId))
        return
    }
    val delay = (departMillis - TRAVEL_LEAD_MILLIS - now).coerceAtLeast(0L)
    val request = OneTimeWorkRequestBuilder<TravelReminderWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(Data.Builder().putLong(TRAVEL_BOOKING_KEY, bookingId).build())
        .addTag(TRAVEL_TAG)
        .build()
    workManager.enqueueUniqueWork(travelWorkName(bookingId), ExistingWorkPolicy.REPLACE, request)
}

/** Cancels the reminder for a booking that was deleted. */
fun cancelTravelReminder(context: Context, bookingId: Long) {
    WorkManager.getInstance(context).cancelUniqueWork(travelWorkName(bookingId))
}

/** Cancels every travel reminder, for example before a backup is restored. */
fun cancelAllTravelReminders(context: Context) {
    WorkManager.getInstance(context).cancelAllWorkByTag(TRAVEL_TAG)
}

/** Sends the reminder for one booking, if it still exists and has not left yet. */
class TravelReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val bookingId = inputData.getLong(TRAVEL_BOOKING_KEY, 0L)
        val db = AppDatabase.create(context)
        try {
            val booking = db.travelDao().getById(bookingId) ?: return Result.success()
            if (booking.departMillis <= System.currentTimeMillis()) return Result.success()

            ensureChannel(context)
            val departs = "${dateLabel(booking.departMillis)} ${SimpleDateFormat("HH:mm", Locale.US).format(Date(booking.departMillis))}"
            val text = context.getString(
                R.string.travel_reminder_text,
                context.getString(booking.mode.labelRes),
                booking.fromPlace,
                booking.toPlace,
                departs,
            )
            notify(context, TRAVEL_NOTIFICATION_BASE + bookingId.toInt(), context.getString(R.string.travel_reminder_title), text)
        } finally {
            db.close()
        }
        return Result.success()
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
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
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
