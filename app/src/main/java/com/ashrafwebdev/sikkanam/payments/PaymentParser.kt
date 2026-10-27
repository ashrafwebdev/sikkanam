package com.ashrafwebdev.sikkanam.payments

import java.math.BigDecimal
import java.math.RoundingMode

/** A payment that was seen in a payment app's notification and is waiting for the user to confirm it. */
data class DetectedPayment(
    val key: String,
    val amountCents: Long,
    val payee: String,
    val app: String,
    val detectedMillis: Long,
)

/** Payment apps whose notifications are read. Only these packages are ever looked at. */
private val supportedApps = mapOf(
    "com.google.android.apps.nbu.paisa.user" to "Google Pay",
    "com.phonepe.app" to "PhonePe",
)

fun supportedAppLabel(packageName: String): String? = supportedApps[packageName]

private val amountRegex = Regex("""(?:₹|Rs\.?|INR)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
private val debitRegex = Regex("""\b(paid|sent|debited|payment of)\b""", RegexOption.IGNORE_CASE)
private val incomingRegex = Regex("""\b(received|credited)\b""", RegexOption.IGNORE_CASE)
private val toRegex = Regex("""\bto\s+(.+)$""", RegexOption.IGNORE_CASE)

/**
 * Reads one notification from a supported payment app. Returns null when it is not a payment the
 * user made, or when the amount cannot be found. Only the parsed fields are kept, never the raw text.
 */
fun parsePayment(
    packageName: String,
    title: String,
    text: String,
    key: String,
    nowMillis: Long,
): DetectedPayment? {
    val app = supportedAppLabel(packageName) ?: return null
    val body = "$title $text".trim()
    if (!debitRegex.containsMatchIn(body) || incomingRegex.containsMatchIn(body)) return null

    val amountText = amountRegex.find(body)?.groupValues?.get(1) ?: return null
    val cents = BigDecimal(amountText.replace(",", ""))
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .toLong()
    if (cents <= 0L) return null

    // "Paid ₹500 to Ravi" gives Ravi. Otherwise the notification title is usually the payee.
    val payee = toRegex.find(body)?.groupValues?.get(1)
        ?.substringBefore(" on ")
        ?.trim()
        ?.trimEnd('.', '!')
        ?: title.trim()
    return DetectedPayment(
        key = key,
        amountCents = cents,
        payee = payee.ifBlank { "Unknown payee" }.take(60),
        app = app,
        detectedMillis = nowMillis,
    )
}
