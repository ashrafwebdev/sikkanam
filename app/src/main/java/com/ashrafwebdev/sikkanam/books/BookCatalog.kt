package com.ashrafwebdev.sikkanam.books

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Islamic books: the list the developer publishes on smatechsolution.com, and any ebook the user
 * imports. Every book on the phone is a copy kept in the app's private folder, so reading works offline.
 *
 * Shape of the published list:
 * {"books":[{"id":"...","title":"...","author":"...","description":"...","format":"epub|pdf|txt",
 *   "url":"https://...","size":12345}]}
 */
const val BOOKS_INDEX_URL = "https://smatechsolution.com/sikkanam/books.json"

/** The formats the reader can open. */
val SUPPORTED_FORMATS = listOf("epub", "pdf", "txt")

private const val INDEX_FILE = "books_index.json"
private const val LOCAL_FILE = "books_local.json"
private const val BOOK_DIR = "books"
private val SAFE_ID = Regex("[A-Za-z0-9_-]+")

/** A book listed on the website. It is not on the phone until the user downloads it. */
data class RemoteBook(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val format: String,
    val url: String,
    val size: Long,
)

/** A book on this phone, either downloaded from the website or imported by the user. */
data class LocalBook(
    val id: String,
    val title: String,
    val author: String,
    val format: String,
    val fileName: String,
)

/** The book list saved on the phone, or empty if it has not been downloaded yet. */
fun loadRemoteIndex(context: Context): List<RemoteBook> {
    val file = File(context.filesDir, INDEX_FILE)
    if (!file.exists()) return emptyList()
    return try {
        parseIndex(file.readText())
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * Downloads the book list and saves it. The text is checked before the saved copy is replaced,
 * so a bad download keeps the list already on the phone. Returns true if the list was saved.
 */
suspend fun refreshRemoteIndex(context: Context, url: String = BOOKS_INDEX_URL): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val text = fetchText(url)
            parseIndex(text)
            File(context.filesDir, INDEX_FILE).writeText(text)
            true
        } catch (e: Exception) {
            false
        }
    }

/** The books saved on this phone. */
fun loadLocalBooks(context: Context): List<LocalBook> {
    val file = File(context.filesDir, LOCAL_FILE)
    if (!file.exists()) return emptyList()
    return try {
        val array = JSONArray(file.readText())
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            LocalBook(
                id = o.getString("id"),
                title = o.getString("title"),
                author = o.optString("author", ""),
                format = o.getString("format"),
                fileName = o.getString("fileName"),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}

/** The file that holds the text of [book] on this phone. */
fun bookFile(context: Context, book: LocalBook): File = File(bookDir(context), book.fileName)

/**
 * Downloads a book from the list into the app's folder. The file is checked before it is kept,
 * so a broken download is never shown as a book. Returns the saved book, or null if it fails.
 */
suspend fun downloadBook(context: Context, book: RemoteBook): LocalBook? = withContext(Dispatchers.IO) {
    val fileName = "${book.id}.${book.format}"
    val target = File(bookDir(context), fileName)
    val part = File(bookDir(context), "$fileName.part")
    try {
        val connection = URL(book.url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
        } finally {
            connection.disconnect()
        }
        if (!isReadable(part, book.format)) {
            part.delete()
            return@withContext null
        }
        target.delete()
        if (!part.renameTo(target)) {
            part.delete()
            return@withContext null
        }
    } catch (e: Exception) {
        part.delete()
        return@withContext null
    }
    val local = LocalBook(book.id, book.title, book.author, book.format, fileName)
    saveLocalBooks(context, loadLocalBooks(context).filter { it.id != book.id } + local)
    local
}

/**
 * Copies a file the user picked into the app's folder. Returns the saved book, or null if the file
 * is not an EPUB, PDF or TXT file that can be opened.
 */
suspend fun importBook(context: Context, source: Uri): LocalBook? = withContext(Dispatchers.IO) {
    val name = displayName(context, source)
    val format = formatFromName(name) ?: formatFromMime(context.contentResolver.getType(source))
        ?: return@withContext null
    val id = UUID.randomUUID().toString()
    val fileName = "$id.$format"
    val target = File(bookDir(context), fileName)
    val copied = try {
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { input.copyTo(it) }
        } != null
    } catch (e: Exception) {
        false
    }
    if (!copied || !isReadable(target, format)) {
        target.delete()
        return@withContext null
    }
    val title = name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: id
    val local = LocalBook(id, title, "", format, fileName)
    saveLocalBooks(context, loadLocalBooks(context) + local)
    local
}

/**
 * Books shipped inside the app (in assets/books). Each one is copied to the phone when its [version]
 * is newer than the copy already installed. Removing a bundled book from the phone keeps it removed
 * until a newer version is shipped.
 */
private data class BundledBook(val id: String, val title: String, val asset: String, val version: Int)

private val BUNDLED_BOOKS = listOf(
    BundledBook("six-principles-en", "The Six Principles (Draft)", "books/six-principles-en.epub", 1),
    BundledBook("six-principles-ta", "ஆறு அடிப்படைகள் (வரைவு)", "books/six-principles-ta.epub", 1),
)

private const val BUNDLED_PREFS = "bundled_books"

/** Copies the books shipped with the app onto the phone, when they are new or updated. */
suspend fun installBundledBooks(context: Context) = withContext(Dispatchers.IO) {
    val prefs = context.getSharedPreferences(BUNDLED_PREFS, Context.MODE_PRIVATE)
    BUNDLED_BOOKS.forEach { book ->
        if (prefs.getInt(book.id, 0) >= book.version) return@forEach
        val fileName = "${book.id}.epub"
        val target = File(bookDir(context), fileName)
        try {
            context.assets.open(book.asset).use { input -> target.outputStream().use { input.copyTo(it) } }
        } catch (e: Exception) {
            return@forEach
        }
        val local = LocalBook(book.id, book.title, "", "epub", fileName)
        saveLocalBooks(context, loadLocalBooks(context).filter { it.id != book.id } + local)
        prefs.edit().putInt(book.id, book.version).apply()
    }
}

/** Deletes the copy of [book] from the phone, and its saved reading place. */
fun removeBook(context: Context, book: LocalBook) {
    saveLocalBooks(context, loadLocalBooks(context).filter { it.id != book.id })
    bookFile(context, book).delete()
    ReadingProgress.clear(context, book.id)
}

private fun bookDir(context: Context): File = File(context.filesDir, BOOK_DIR).apply { mkdirs() }

private fun saveLocalBooks(context: Context, books: List<LocalBook>) {
    val array = JSONArray()
    books.forEach { b ->
        array.put(
            JSONObject()
                .put("id", b.id)
                .put("title", b.title)
                .put("author", b.author)
                .put("format", b.format)
                .put("fileName", b.fileName)
        )
    }
    File(context.filesDir, LOCAL_FILE).writeText(array.toString())
}

private fun fetchText(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

/** Entries with a bad id, an unsupported format or a non-https link are skipped. */
private fun parseIndex(text: String): List<RemoteBook> {
    val books = JSONObject(text).getJSONArray("books")
    return List(books.length()) { i -> books.getJSONObject(i) }.mapNotNull { o ->
        val id = o.getString("id")
        val format = o.getString("format").lowercase()
        val url = o.getString("url")
        if (!SAFE_ID.matches(id) || format !in SUPPORTED_FORMATS || !url.startsWith("https://")) {
            null
        } else {
            RemoteBook(
                id = id,
                title = o.getString("title"),
                author = o.optString("author", ""),
                description = o.optString("description", ""),
                format = format,
                url = url,
                size = o.optLong("size", 0L),
            )
        }
    }
}

private fun displayName(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
} catch (e: Exception) {
    null
}

private fun formatFromName(name: String?): String? =
    name?.substringAfterLast('.', "")?.lowercase()?.takeIf { it in SUPPORTED_FORMATS }

private fun formatFromMime(mime: String?): String? = when (mime) {
    "application/epub+zip" -> "epub"
    "application/pdf" -> "pdf"
    "text/plain" -> "txt"
    else -> null
}
