package com.ashrafwebdev.sikkanam.payments

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ashrafwebdev.sikkanam.MainActivity

private const val CHANNEL_ID = "payments"

/**
 * Reads payment notifications from Google Pay and PhonePe, only when the user has turned this on
 * and allowed notification access. It never reads other apps, and it never sends anything off the phone.
 */
class PaymentNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isPaymentCaptureOn(this)) return
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val payment = parsePayment(
            packageName = sbn.packageName,
            title = title,
            text = text,
            key = "${sbn.key}:${sbn.postTime}",
            nowMillis = System.currentTimeMillis(),
        ) ?: return

        if (PendingPayments.add(this, payment)) tellUser(payment)
    }

    /** Shows a short notification so the user knows a payment is waiting to be added. */
    private fun tellUser(payment: DetectedPayment) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Payments found", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Payment found: ${payment.app}")
            .setContentText("${payment.payee}. Open Sikkanam to add it.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(payment.key.hashCode(), notification)
    }
}
