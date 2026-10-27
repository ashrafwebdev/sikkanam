package com.ashrafwebdev.sikkanam.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS = "display"
private const val KEY_TEXT_SIZE = "text_size"

/** How much bigger or smaller the app's text is, on top of the phone's own font size setting. */
enum class TextSize(val label: String, val factor: Float) {
    SMALL("Small", 0.9f),
    NORMAL("Normal", 1f),
    LARGE("Large", 1.15f),
    EXTRA_LARGE("Extra large", 1.3f),
}

/** The chosen text size. Saved on the device, and applied as soon as it changes. */
object TextSizeSetting {
    private val _size = MutableStateFlow(TextSize.NORMAL)
    val size: StateFlow<TextSize> = _size.asStateFlow()
    private var loaded = false

    @Synchronized
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TEXT_SIZE, null)
        _size.value = saved?.let { runCatching { TextSize.valueOf(it) }.getOrNull() } ?: TextSize.NORMAL
    }

    @Synchronized
    fun set(context: Context, size: TextSize) {
        _size.value = size
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TEXT_SIZE, size.name).apply()
    }
}
