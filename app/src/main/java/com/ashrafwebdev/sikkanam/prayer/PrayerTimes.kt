package com.ashrafwebdev.sikkanam.prayer

import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/** A city the user can pick. Coordinates are in degrees. Indian Standard Time is used for all cities. */
data class City(val name: String, val lat: Double, val lon: Double)

val cities: List<City> = listOf(
    City("Chennai", 13.0827, 80.2707),
    City("Guduvancheri", 12.8500, 80.0600),
    City("Coimbatore", 11.0168, 76.9558),
    City("Madurai", 9.9252, 78.1198),
    City("Trichy", 10.7905, 78.7047),
    City("Salem", 11.6643, 78.1460),
    City("Erode", 11.3410, 77.7172),
    City("Vellore", 12.9165, 79.1325),
    City("Thanjavur", 10.7870, 79.1378),
    City("Tirunelveli", 8.7139, 77.7567),
    City("Kanyakumari", 8.0883, 77.5385),
)

/**
 * Standard: Asr starts when an object's shadow equals its length.
 * Hanafi: Asr starts when the shadow is twice the object's length (later in the day).
 */
enum class AsrMethod { STANDARD, HANAFI }

/** Prayer times as minutes after local midnight. */
data class PrayerTimes(
    val fajr: Double,
    val sunrise: Double,
    val zuhr: Double,
    val asr: Double,
    val maghrib: Double,
    val isha: Double,
)

private const val FAJR_ANGLE = -18.0
private const val ISHA_ANGLE = -18.0
private const val SUNRISE_ANGLE = -0.833
private const val INDIA_TIMEZONE_HOURS = 5.5

/** Calculates the five prayers and sunrise for [city] on the given date, using the standard solar position formulas. */
fun calculatePrayerTimes(city: City, year: Int, month: Int, day: Int, asr: AsrMethod): PrayerTimes {
    val calendar = Calendar.getInstance().apply { set(year, month, day) }
    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

    // Solar declination and equation of time, taken at solar noon.
    val gamma = 2 * PI / 365 * (dayOfYear - 1)
    val eqTime = 229.18 * (
        0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
            0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma)
        )
    val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
        0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
        0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)

    val latRad = city.lat * PI / 180
    val noon = 720 - 4 * city.lon - eqTime + INDIA_TIMEZONE_HOURS * 60

    // Minutes from solar noon to the time the sun is at [altitude] degrees.
    fun offset(altitude: Double): Double {
        val c = (sin(altitude * PI / 180) - sin(latRad) * sin(decl)) / (cos(latRad) * cos(decl))
        val angle = acos(c.coerceIn(-1.0, 1.0)) * 180 / PI
        return 4 * angle
    }

    // Asr altitude: the sun is low enough that the shadow reaches the chosen multiple of the object's length.
    val shadowFactor = if (asr == AsrMethod.HANAFI) 2.0 else 1.0
    val asrAltitude = atan(1.0 / (shadowFactor + abs(tan(latRad - decl)))) * 180 / PI

    return PrayerTimes(
        fajr = noon - offset(FAJR_ANGLE),
        sunrise = noon - offset(SUNRISE_ANGLE),
        zuhr = noon,
        asr = noon + offset(asrAltitude),
        maghrib = noon + offset(SUNRISE_ANGLE),
        isha = noon + offset(ISHA_ANGLE),
    )
}

/** Formats minutes after midnight as a 12-hour time, for example "5:12 AM". */
fun formatPrayerTime(minutes: Double): String {
    val total = minutes.roundToInt().mod(24 * 60)
    val hour24 = total / 60
    val minute = total % 60
    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    val suffix = if (hour24 < 12) "AM" else "PM"
    return "%d:%02d %s".format(hour12, minute, suffix)
}
