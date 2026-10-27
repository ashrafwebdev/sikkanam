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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(bookId).apply()
    }
}
