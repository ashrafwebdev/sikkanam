package com.ashrafwebdev.sikkanam.ui

import android.content.Context

private const val PREFS = "display"
private const val KEY_TEXT_SIZE = "text_size"

/** How much bigger or smaller the app's text is, on top of the phone's own font size setting. */
enum class TextSize(val label: String, val factor: Float) {
    SMALL("Small", 0.9f),
    NORMAL("Normal", 1f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Extra large", 1.3f),
}

/**
 * The chosen text size, saved on the device. It is applied when the activity starts (see
 * MainActivity.attachBaseContext), so it reaches every view, including menus and dialogs.
 */
object TextSizeSetting {
    fun read(context: Context): TextSize {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TEXT_SIZE, null)
        return saved?.let { runCatching { TextSize.valueOf(it) }.getOrNull() } ?: TextSize.NORMAL
    }

    fun save(context: Context, size: TextSize) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TEXT_SIZE, size.name).apply()
    }
}
