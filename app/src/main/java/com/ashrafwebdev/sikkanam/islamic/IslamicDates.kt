package com.ashrafwebdev.sikkanam.islamic

import android.icu.util.Calendar
import android.icu.util.ULocale

/** A key date in the Islamic year. [month] is 0-based, as in the Islamic calendar (0 = Muharram, 8 = Ramadan). */
data class IslamicEvent(val nameKey: String, val month: Int, val day: Int)

/** Key dates. Many of them are observed on the same day, but the exact day depends on moon sighting. */
val islamicEvents: List<IslamicEvent> = listOf(
    IslamicEvent("islamic_new_year", 0, 1),
    IslamicEvent("islamic_ashura", 0, 10),
    IslamicEvent("islamic_isra_miraj", 6, 27),
    IslamicEvent("islamic_shab_e_barat", 7, 15),
    IslamicEvent("islamic_ramadan_start", 8, 1),
    IslamicEvent("islamic_laylat_al_qadr", 8, 27),
    IslamicEvent("islamic_eid_al_fitr", 9, 1),
    IslamicEvent("islamic_arafah", 11, 9),
    IslamicEvent("islamic_eid_al_adha", 11, 10),
    IslamicEvent("islamic_mawlid", 2, 12),
)

/** An event on its next Gregorian date. [millis] is the start of that day, local time. */
data class UpcomingEvent(val event: IslamicEvent, val millis: Long)

/**
 * The next [count] key dates from today, using the Islamic civil calendar. A date that has already
 * passed this Islamic year is moved to the next year.
 */
fun upcomingIslamicEvents(nowMillis: Long, count: Int): List<UpcomingEvent> {
    val locale = ULocale("en@calendar=islamic-civil")
    val today = Calendar.getInstance(locale).apply { timeInMillis = nowMillis }
    val hijriYear = today.get(Calendar.YEAR)
    val startOfToday = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val upcoming = islamicEvents.flatMap { event ->
        listOf(hijriYear, hijriYear + 1).map { year ->
            val cal = Calendar.getInstance(locale)
            cal.clear()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, event.month)
            cal.set(Calendar.DAY_OF_MONTH, event.day)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            UpcomingEvent(event, cal.timeInMillis)
        }
    }
    return upcoming
        .filter { it.millis >= startOfToday }
        .sortedBy { it.millis }
        .take(count)
}
