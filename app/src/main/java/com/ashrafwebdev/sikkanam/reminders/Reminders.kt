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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.AppDatabase
import com.ashrafwebdev.sikkanam.data.TransactionType
import com.ashrafwebdev.sikkanam.ui.formatMoney
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "sikkanam_reminders"
private const val PREFS = "sikkanam_prefs"
private const val KEY_ZAKAT_DATE = "zakat_due_millis"
private const val WORK_NAME = "sikkanam_daily_reminders"

/** Remembers the zakat due date the user set, as start-of-day epoch millis. Zero means not set. */
fun zakatDueDate(context: Context): Long =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_ZAKAT_DATE, 0L)

fun setZakatDueDate(context: Context, millis: Long) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_ZAKAT_DATE, millis).apply()
}

/** Runs once a day, and notifies about bills and zakat due tomorrow. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        ensureChannel(context)

        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val tomorrowDay = tomorrow.get(Calendar.DAY_OF_MONTH)
        val tomorrowStart = tomorrow.apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val db = AppDatabase.create(context)
        try {
            // A monthly bill is a recurring expense template. Remind when its day-of-month is tomorrow.
            val bills = db.transactionDao().getAll().filter {
                it.recurringMonthly && it.sourceId == null && it.type == TransactionType.EXPENSE &&
                    Calendar.getInstance().apply { timeInMillis = it.dateMillis }.get(Calendar.DAY_OF_MONTH) == tomorrowDay
            }
            bills.forEach { bill ->
                notify(context, bill.id.toInt(), context.getString(R.string.reminder_bill_title),
                    "${bill.category}: ${formatMoney(bill.amountCents)} is due tomorrow")
            }
        } finally {
            db.close()
        }

        val zakat = zakatDueDate(context)
        if (zakat != 0L && zakat == tomorrowStart) {
            notify(context, 1_000_001, context.getString(R.string.reminder_zakat_title), context.getString(R.string.reminder_zakat_body))
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
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/** Schedules the daily check. Safe to call on every launch; the existing schedule is kept. */
fun scheduleReminders(context: Context) {
    val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}

/** Whether the app may post notifications (always true before Android 13). */
fun canNotify(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
