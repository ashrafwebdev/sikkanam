package com.ashrafwebdev.sikkanam.books

import android.content.Context

/**
 * Where the reader stopped in each book, kept on this phone. The number is the first item on
 * screen: a page for PDF, a paragraph for EPUB and TXT.
 */
object ReadingProgress {
    private const val PREFS = "reading_progress"

    fun load(context: Context, bookId: String): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(bookId, 0)

    fun save(context: Context, bookId: String, position: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(bookId, position).apply()
    }

    fun clear(context: Context, bookId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(bookId)
            .remove(bookmarkKey(bookId))
            .remove(finishedKey(bookId))
            .apply()
    }

    /** True once the reader has reached the last page or paragraph of the book. */
    fun isFinished(context: Context, bookId: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(finishedKey(bookId), false)

    fun markFinished(context: Context, bookId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(finishedKey(bookId), true).apply()
    }

    /** Starts a finished book again from the beginning. Bookmarks are kept. */
    fun restart(context: Context, bookId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(bookId, 0)
            .putBoolean(finishedKey(bookId), false)
            .apply()
    }

    private fun finishedKey(bookId: String) = "finished_$bookId"

    /** The places marked with a bookmark in this book, in reading order. */
    fun bookmarks(context: Context, bookId: String): List<Int> {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(bookmarkKey(bookId), "").orEmpty()
        return saved.split(",").mapNotNull { it.toIntOrNull() }.distinct().sorted()
    }

    fun addBookmark(context: Context, bookId: String, position: Int) {
        val updated = bookmarks(context, bookId) + position
        saveBookmarks(context, bookId, updated)
    }

    fun clearBookmarks(context: Context, bookId: String) {
        saveBookmarks(context, bookId, emptyList())
    }

    private fun saveBookmarks(context: Context, bookId: String, positions: List<Int>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(bookmarkKey(bookId), positions.distinct().sorted().joinToString(","))
            .apply()
    }

    private fun bookmarkKey(bookId: String) = "bookmarks_$bookId"
}

/** The text size the reader uses for EPUB and TXT books, as a step from 0 (smallest) to 4 (largest). */
object ReaderFontSize {
    private const val PREFS = "reader_settings"
    private const val KEY = "font_step"
    const val MIN_STEP = 0
    const val MAX_STEP = 4
    const val DEFAULT_STEP = 1

    fun load(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, DEFAULT_STEP).coerceIn(MIN_STEP, MAX_STEP)

    fun save(context: Context, step: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, step.coerceIn(MIN_STEP, MAX_STEP)).apply()
    }
}

/** The page colours of the reader for EPUB and TXT books: light (the app's own colours), sepia or dark. */
object ReaderTheme {
    private const val PREFS = "reader_settings"
    private const val KEY = "theme"
    const val LIGHT = 0
    const val SEPIA = 1
    const val DARK = 2

    fun load(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, LIGHT).coerceIn(LIGHT, DARK)

    /** The next theme after [theme], wrapping from dark back to light. */
    fun next(theme: Int): Int = if (theme >= DARK) LIGHT else theme + 1

    fun save(context: Context, theme: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, theme.coerceIn(LIGHT, DARK)).apply()
    }
}
