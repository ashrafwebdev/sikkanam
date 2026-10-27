package com.ashrafwebdev.sikkanam.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimesTest {

    private val chennai = cities.first { it.name == "Chennai" }

    @Test
    fun prayersAreInOrderInChennai() {
        val t = calculatePrayerTimes(chennai, 2026, 9, 27, AsrMethod.STANDARD)
        assertTrue(t.fajr < t.sunrise)
        assertTrue(t.sunrise < t.zuhr)
        assertTrue(t.zuhr < t.asr)
        assertTrue(t.asr < t.maghrib)
        assertTrue(t.maghrib < t.isha)
    }

    @Test
    fun zuhrIsAroundNoonInChennai() {
        // Chennai is east of the Indian Standard Time meridian, so solar noon is a little after 12:00.
        val t = calculatePrayerTimes(chennai, 2026, 9, 27, AsrMethod.STANDARD)
        assertTrue(t.zuhr > 11 * 60 + 50 && t.zuhr < 12 * 60 + 30)
    }

    @Test
    fun hanafiAsrIsLaterThanStandardAsr() {
        val standard = calculatePrayerTimes(chennai, 2026, 9, 27, AsrMethod.STANDARD)
        val hanafi = calculatePrayerTimes(chennai, 2026, 9, 27, AsrMethod.HANAFI)
        assertTrue(hanafi.asr > standard.asr)
        assertTrue(hanafi.asr < hanafi.maghrib)
    }

    @Test
    fun formatsTwelveHourTime() {
        assertEquals("12:05 PM", formatPrayerTime(12 * 60 + 5.0))
        assertEquals("12:00 AM", formatPrayerTime(0.0))
        assertEquals("5:30 AM", formatPrayerTime(5 * 60 + 30.0))
    }
}
