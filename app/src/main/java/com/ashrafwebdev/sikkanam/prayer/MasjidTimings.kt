package com.ashrafwebdev.sikkanam.prayer

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MASJID_PREFS = "masjid_timings"
private const val KEY_ACTIVE = "active"

/**
 * The masjids the user prays in, up to five. Each has its own name and its own adhan, iqamah and
 * prayer times, typed in by the user as hh:mm. One masjid is active and is shown on the Home prayer card.
 * Everything is kept on the phone only. A masjid slot with no name is not in use.
 */
object MasjidTimings {
    const val MAX_MASJIDS = 5

    /** The prayers that have masjid adhan and iqamah, in order. Jumah is shown on Fridays only. */
    val prayers = listOf("fajr", "zuhr", "asr", "maghrib", "isha", "jumah")

    /** Every row on the Home prayer card that a masjid can give its own time for. */
    val timeNames = listOf("subuh_sadiq", "fajr", "sunrise", "zuhr", "asr", "sunset", "maghrib", "isha", "jumah")

    private fun prefs(context: Context) = context.getSharedPreferences(MASJID_PREFS, Context.MODE_PRIVATE)

    /**
     * Key for one saved value: [kind] is "adhan", "iqama" or "time", [prayer] is a name from [prayers] or
     * [timeNames]. Masjid 0 keeps the keys used before there were several masjids.
     */
    fun key(kind: String, prayer: String, masjid: Int): String =
        if (masjid == 0) "${kind}_$prayer" else "m${masjid}_${kind}_$prayer"

    fun activeIndex(context: Context): Int = prefs(context).getInt(KEY_ACTIVE, 0)

    fun setActive(context: Context, masjid: Int) {
        prefs(context).edit().putInt(KEY_ACTIVE, masjid).apply()
    }

    fun name(context: Context, masjid: Int): String = prefs(context).getString("name_$masjid", "").orEmpty()

    /** The masjids in use, as pairs of slot and name, in slot order. */
    fun masjids(context: Context): List<Pair<Int, String>> =
        (0 until MAX_MASJIDS).map { it to name(context, it) }.filter { it.second.isNotBlank() }

    /** Adds a masjid in the first free slot. Returns its slot, or null when five are already in use. */
    fun add(context: Context, name: String): Int? {
        val slot = (0 until MAX_MASJIDS).firstOrNull { name(context, it).isBlank() } ?: return null
        prefs(context).edit().putString("name_$slot", name.trim()).apply()
        if (masjids(context).size == 1) setActive(context, slot)
        return slot
    }

    /** Removes a masjid and all its times. If it was the active one, the first remaining masjid becomes active. */
    fun remove(context: Context, masjid: Int) {
        val editor = prefs(context).edit()
        editor.remove("name_$masjid")
        editor.remove("range_from_$masjid")
        editor.remove("range_to_$masjid")
        keysFor(masjid).forEach { editor.remove(it) }
        editor.apply()
        deletePhoto(context, masjid)
        if (activeIndex(context) == masjid) {
            setActive(context, masjids(context).firstOrNull()?.first ?: 0)
        }
    }

    private fun keysFor(masjid: Int): List<String> = listOf("adhan", "iqama", "time").flatMap { kind ->
        (prayers + timeNames).map { key(kind, it, masjid) }
    }

    fun get(context: Context, kind: String, prayer: String, masjid: Int = activeIndex(context)): String =
        prefs(context).getString(key(kind, prayer, masjid), "").orEmpty()

    /** Saves the times of one masjid. Keys are the ones made by [key] for that masjid. */
    fun saveAll(context: Context, masjid: Int, values: Map<String, String>) {
        val editor = prefs(context).edit()
        values.forEach { (key, value) -> editor.putString(key, value.trim()) }
        editor.apply()
    }

    /** The dates a masjid's times apply, as typed: from and to in dd/mm/yyyy. Empty means no limit. */
    fun rangeText(context: Context, masjid: Int): Pair<String, String> {
        val from = prefs(context).getLong("range_from_$masjid", 0L)
        val to = prefs(context).getLong("range_to_$masjid", 0L)
        return format(from) to format(to)
    }

    /** Saves the dates a masjid's times apply. Returns false when a date is not valid or the range is backwards. */
    fun saveRange(context: Context, masjid: Int, fromText: String, toText: String): Boolean {
        val from = if (fromText.isBlank()) 0L else parseDate(fromText) ?: return false
        val to = if (toText.isBlank()) 0L else parseDate(toText) ?: return false
        if (from != 0L && to != 0L && to < from) return false
        prefs(context).edit().putLong("range_from_$masjid", from).putLong("range_to_$masjid", to).apply()
        return true
    }

    /** True when the masjid's times apply today. With no dates set they always apply. */
    fun inRange(context: Context, masjid: Int, now: Long = System.currentTimeMillis()): Boolean {
        val from = prefs(context).getLong("range_from_$masjid", 0L)
        val to = prefs(context).getLong("range_to_$masjid", 0L)
        val day = now - now % DAY_MILLIS
        if (from != 0L && day < from) return false
        // The to date is included in the range, so it lasts until the end of that day.
        if (to != 0L && day > to) return false
        return true
    }

    /** The saved value for a row, or empty when the masjid's dates do not include today. */
    fun currentValue(context: Context, kind: String, prayer: String, masjid: Int = activeIndex(context)): String =
        if (inRange(context, masjid)) get(context, kind, prayer, masjid) else ""

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    private fun parseDate(text: String): Long? = runCatching {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }.parse(text.trim())?.time
    }.getOrNull()

    private fun format(millis: Long): String =
        if (millis == 0L) "" else SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(millis))

    /** The photo file of a masjid's timings, kept in the app's private folder. Photos are not in the backup. */
    fun photoFile(context: Context, masjid: Int): File = File(context.filesDir, "masjid_photos/masjid_$masjid.jpg")

    fun hasPhoto(context: Context, masjid: Int): Boolean = photoFile(context, masjid).exists()

    /** Copies the photo the user picked into the app's folder. Returns false if it could not be copied. */
    fun savePhoto(context: Context, masjid: Int, source: Uri): Boolean = try {
        val target = photoFile(context, masjid)
        target.parentFile?.mkdirs()
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { input.copyTo(it) }
        } != null
    } catch (e: Exception) {
        false
    }

    fun deletePhoto(context: Context, masjid: Int) {
        photoFile(context, masjid).delete()
    }

    /** The masjid's own time for a row as minutes after midnight, or null when it is not entered for today. */
    fun manualMinutes(context: Context, name: String, masjid: Int = activeIndex(context)): Double? {
        val text = currentValue(context, "time", name, masjid).trim()
        if (!isValid(text)) return null
        return text.substring(0, 2).toInt() * 60.0 + text.substring(3, 5).toInt()
    }

    /** True for a time written as hh:mm, from 00:00 to 23:59. */
    fun isValid(text: String): Boolean = Regex("([01]\\d|2[0-3]):[0-5]\\d").matches(text.trim())
}
