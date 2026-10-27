package com.ashrafwebdev.sikkanam.prayer

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** The Kaaba in Makkah, in degrees. */
private const val KAABA_LAT = 21.422487
private const val KAABA_LON = 39.826206

/**
 * The direction of the Kaaba from [lat], [lon], in degrees clockwise from true north (0 to 360).
 * Uses the initial great-circle bearing.
 */
fun qiblaBearing(lat: Double, lon: Double): Double {
    val phi1 = Math.toRadians(lat)
    val phi2 = Math.toRadians(KAABA_LAT)
    val deltaLambda = Math.toRadians(KAABA_LON - lon)
    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}
