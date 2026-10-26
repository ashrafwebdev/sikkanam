package com.ashrafwebdev.sikkanam.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.UUID

private const val RECEIPT_DIR = "receipts"

/**
 * Copies a picked photo into the app's private folder, so it stays available even if the
 * original is moved or deleted. Returns the stored file name, or null if the copy fails.
 */
fun saveReceipt(context: Context, source: Uri): String? = try {
    val dir = File(context.filesDir, RECEIPT_DIR).apply { mkdirs() }
    val name = "${UUID.randomUUID()}.jpg"
    context.contentResolver.openInputStream(source)?.use { input ->
        File(dir, name).outputStream().use { output -> input.copyTo(output) }
    }
    name
} catch (e: Exception) {
    null
}

/** Loads a stored receipt, scaled down so it stays light in memory. Null if it's missing. */
fun loadReceipt(context: Context, name: String): ImageBitmap? {
    val file = File(File(context.filesDir, RECEIPT_DIR), name)
    if (!file.exists()) return null
    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
    return BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
}

/** Removes a stored receipt. Missing files are ignored. */
fun deleteReceipt(context: Context, name: String) {
    File(File(context.filesDir, RECEIPT_DIR), name).delete()
}
