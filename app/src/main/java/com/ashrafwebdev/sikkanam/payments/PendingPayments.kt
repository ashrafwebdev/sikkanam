package com.ashrafwebdev.sikkanam.payments

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val PREFS = "payments"
private const val KEY_CAPTURE_ON = "capture_on"
private const val KEY_ITEMS = "pending"

/** Whether the user has turned on payment alerts. Off by default. */
fun isPaymentCaptureOn(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CAPTURE_ON, false)

fun setPaymentCaptureOn(context: Context, on: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_CAPTURE_ON, on).apply()
}

/** True when the user has allowed Sikkanam to read notifications in phone settings. */
fun isNotificationAccessGranted(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/**
 * Payments waiting for the user to confirm. Nothing becomes a transaction until the user taps Add.
 * Kept in SharedPreferences so the list survives an app restart.
 */
object PendingPayments {
    private val _items = MutableStateFlow<List<DetectedPayment>>(emptyList())
    val items: StateFlow<List<DetectedPayment>> = _items.asStateFlow()
    private var loaded = false

    @Synchronized
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        _items.value = readAll(context)
    }

    /** Adds a payment unless one with the same key is already waiting. Returns true when it was added. */
    @Synchronized
    fun add(context: Context, payment: DetectedPayment): Boolean {
        load(context)
        if (_items.value.any { it.key == payment.key }) return false
        _items.value = listOf(payment) + _items.value
        writeAll(context)
        return true
    }

    @Synchronized
    fun remove(context: Context, key: String) {
        load(context)
        _items.value = _items.value.filterNot { it.key == key }
        writeAll(context)
    }

    private fun writeAll(context: Context) {
        val array = JSONArray()
        _items.value.forEach { p ->
            array.put(
                JSONObject()
                    .put("key", p.key)
                    .put("amountCents", p.amountCents)
                    .put("payee", p.payee)
                    .put("app", p.app)
                    .put("detectedMillis", p.detectedMillis),
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private fun readAll(context: Context): List<DetectedPayment> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                DetectedPayment(
                    key = o.getString("key"),
                    amountCents = o.getLong("amountCents"),
                    payee = o.getString("payee"),
                    app = o.getString("app"),
                    detectedMillis = o.getLong("detectedMillis"),
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }
}
