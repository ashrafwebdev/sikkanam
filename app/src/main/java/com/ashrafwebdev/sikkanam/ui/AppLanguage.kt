package com.ashrafwebdev.sikkanam.ui

import android.content.Context

private const val PREFS = "display"
private const val KEY_LANGUAGE = "language"

/** The language the app shows. SYSTEM follows the phone's language. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    TAMIL("ta"),
}

/**
 * The chosen app language, saved on the device. It is applied when the activity starts (see
 * MainActivity.attachBaseContext), the same way as the text size.
 */
object AppLanguageSetting {
    fun read(context: Context): AppLanguage {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
        return saved?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM
    }

    fun save(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LANGUAGE, language.name).apply()
    }
}
