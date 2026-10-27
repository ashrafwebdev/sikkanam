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
import com.ashrafwebdev.sikkanam.data.DebtDirection
import com.ashrafwebdev.sikkanam.prayer.AsrMethod
import com.ashrafwebdev.sikkanam.prayer.PrayerSettings
import com.ashrafwebdev.sikkanam.prayer.calculatePrayerTimes
import com.ashrafwebdev.sikkanam.ui.formatMoney
import com.ashrafwebdev.sikkanam.ui.startOfDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val ALERT_CHANNEL_ID = "sikkanam_alerts"
private const val ALERT_WORK_NAME = "sikkanam_prayer_and_debt_alerts"
private const val ALERT_PREFS = "alerts_sent"

/** How many minutes before a prayer the reminder is shown. */
private const val LEAD_MINUTES = 10

/** Runs every 15 minutes (the shortest interval Android allows). Sends each reminder once. */
class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        ensureAlertChannel(context)
        val now = System.currentTimeMillis()
        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(now))
        val sent = context.getSharedPreferences(ALERT_PREFS, Context.MODE_PRIVATE)

        if (PrayerSettings.remindersOn(context)) {
            prayerReminders(context, now, dayKey, sent)
        }
        debtReminders(context, now, dayKey, sent)
        return Result.success()
    }

    private fun prayerReminders(context: Context, now: Long, dayKey: String, sent: android.content.SharedPreferences) {
        val city = PrayerSettings.city(context)
        val asr = PrayerSettings.asr(context)
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val times = calculatePrayerTimes(
            city, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH), asr,
        )
        val dayStart = startOfDay(now)
        val prayers = listOf(
            "fajr" to times.fajr,
            "zuhr" to times.zuhr,
            "asr" to times.asr,
            "maghrib" to times.maghrib,
            "isha" to times.isha,
        )
        prayers.forEachIndexed { index, (name, minutes) ->
            val at = dayStart + (minutes * 60_000).toLong()
            val remindFrom = at - LEAD_MINUTES * 60_000L
            // Send once, between the reminder time and 30 minutes after the prayer starts.
            val key = "prayer_${dayKey}_$name"
            if (now in remindFrom..(at + 30 * 60_000L) && !sent.getBoolean(key, false)) {
                sent.edit().putBoolean(key, true).apply()
                val prayerName = context.getString(prayerTitleRes(name))
                val title = context.getString(R.string.alert_prayer_title, prayerName, LEAD_MINUTES)
                notify(context, 2000 + index, title, context.getString(R.string.alert_prayer_body, prayerName))
            }
        }
    }

    private suspend fun debtReminders(context: Context, now: Long, dayKey: String, sent: android.content.SharedPreferences) {
        val db = AppDatabase.create(context)
        try {
            val todayStart = startOfDay(now)
            val tomorrowStart = todayStart + 24L * 60 * 60 * 1000
            db.debtDao().getAll().forEach { debt ->
                if (debt.dueMillis == 0L || debt.remainingCents == 0L) return@forEach
                val dueDay = startOfDay(debt.dueMillis)
                if (dueDay == todayStart || dueDay == tomorrowStart) {
                    val key = "debt_${debt.id}_${dayKey}"
                    if (!sent.getBoolean(key, false)) {
                        sent.edit().putBoolean(key, true).apply()
                        val who = context.getString(
                            if (debt.direction == DebtDirection.LENT) R.string.alert_from else R.string.alert_to,
                        )
                        val whenText = context.getString(
                            if (dueDay == todayStart) R.string.alert_today else R.string.alert_tomorrow,
                        )
                        notify(
                            context,
                            3000 + debt.id.toInt(),
                            context.getString(R.string.alert_debt_title),
                            context.getString(R.string.alert_debt_body, who, debt.person, formatMoney(debt.remainingCents), whenText),
                        )
                    }
                }
            }
        } finally {
            db.close()
        }
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}

private fun prayerTitleRes(name: String): Int = when (name) {
    "fajr" -> R.string.alert_prayer_fajr
    "zuhr" -> R.string.alert_prayer_zuhr
    "asr" -> R.string.alert_prayer_asr
    "maghrib" -> R.string.alert_prayer_maghrib
    else -> R.string.alert_prayer_isha
}

private fun ensureAlertChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            context.getString(R.string.alert_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/** Schedules the 15-minute check for prayer and debt reminders. Safe to call on every launch. */
fun scheduleAlerts(context: Context) {
    val request = PeriodicWorkRequestBuilder<AlertWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(ALERT_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
}
