package com.ashrafwebdev.sikkanam.ui

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

/** Parses a typed amount into cents. Returns null for anything that isn't a positive number. */
fun parseCents(text: String): Long? {
    val cents = text.trim().toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: return null
    return cents.takeIf { it > 0 }
}

/**
 * Rupees in the Indian style: ₹1,00,000.00 (groups of two after the first three digits).
 * Negative amounts show the minus sign before the rupee sign.
 */
fun formatMoney(cents: Long): String {
    val rupees = (kotlin.math.abs(cents) / 100).toString()
    val paise = (kotlin.math.abs(cents) % 100).toString().padStart(2, '0')
    val grouped = if (rupees.length <= 3) {
        rupees
    } else {
        rupees.dropLast(3).reversed().chunked(2).joinToString(",").reversed() + "," + rupees.takeLast(3)
    }
    val sign = if (cents < 0) "-" else ""
    return "$sign₹$grouped.$paise"
}

/** Plain amount for editing and export, for example 1500 cents becomes "15.00". */
fun formatPlain(cents: Long): String = String.format(Locale.US, "%.2f", cents / 100.0)

/** Epoch millis of the first day of the month containing [millis], at local midnight. */
fun startOfMonth(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

/** Midnight, local time, of the day containing [millis]. */
fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

/** Moves a month-start value forward or back by [months]. */
fun addMonths(monthStart: Long, months: Int): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart }
    cal.add(Calendar.MONTH, months)
    return cal.timeInMillis
}

fun monthLabel(monthStart: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(monthStart))

/** Only the digits of a date being typed, up to eight. [DigitsVisualTransformation] shows the slashes. */
fun dateDigits(input: String): String = input.filter { it.isDigit() }.take(8)

/** The date as dd/mm/yyyy, from digits or from text that already has its slashes. */
fun dateWithSlashes(input: String): String = buildString {
    dateDigits(input).forEachIndexed { index, digit ->
        if (index == 2 || index == 4) append('/')
        append(digit)
    }
}

/** Only the digits of a time being typed, up to four. [DigitsVisualTransformation] shows the colon. */
fun timeDigits(input: String): String = input.filter { it.isDigit() }.take(4)

/** The time as hh:mm, from digits or from text that already has its colon. A time still being typed keeps what is there. */
fun timeWithColon(input: String): String {
    val digits = timeDigits(input)
    return if (digits.length > 2) digits.take(2) + ":" + digits.drop(2) else digits
}

/**
 * Shows the digits of a date or time with its separators, as [display] builds them. The text itself stays
 * digits only, so the cursor keeps its place while the user types.
 */
class DigitsVisualTransformation(private val display: (String) -> String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = display(text.text)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var digitsSeen = 0
                shown.forEachIndexed { index, char ->
                    if (digitsSeen == offset) return index
                    if (char.isDigit()) digitsSeen++
                }
                return shown.length
            }

            override fun transformedToOriginal(offset: Int): Int = shown.take(offset).count { it.isDigit() }
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}

/** Indian date format, for example 26/10/2026. */
fun dateLabel(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("en", "IN")).format(Date(millis))

/** Today's date in the Islamic (Hijri) calendar, for example "14 Jumada II 1448". */
fun hijriToday(): String =
    SimpleDateFormat("d MMMM y", ULocale("en@calendar=islamic-civil"))
        .format(Date(System.currentTimeMillis()))
