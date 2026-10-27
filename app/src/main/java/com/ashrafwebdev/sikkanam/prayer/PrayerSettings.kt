package com.ashrafwebdev.sikkanam.prayer

import android.content.Context

private const val PREFS = "prayer_settings"
private const val KEY_CITY = "city"
private const val KEY_ASR = "asr"

/** The city and Asr method chosen by the user, saved on the phone. Chennai and standard Asr are the defaults. */
object PrayerSettings {
    fun city(context: Context): City {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CITY, null)
        return cities.firstOrNull { it.name == saved } ?: cities.first()
    }

    fun saveCity(context: Context, city: City) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CITY, city.name).apply()
    }

    fun asr(context: Context): AsrMethod {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ASR, null)
        return saved?.let { runCatching { AsrMethod.valueOf(it) }.getOrNull() } ?: AsrMethod.STANDARD
    }

    /** Whether a reminder is shown 10 minutes before each prayer. On by default. */
    fun remindersOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("reminders_on", true)

    fun saveRemindersOn(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("reminders_on", on).apply()
    }

    fun saveAsr(context: Context, method: AsrMethod) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ASR, method.name).apply()
    }
}
