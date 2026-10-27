package com.ashrafwebdev.sikkanam.books

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Html
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.Closeable
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

/** Text of a book as paragraphs, in reading order. EPUB chapters are joined one after another. */
fun readTextBook(file: File, format: String): List<String> = when (format) {
    "epub" -> readEpub(file)
    else -> readPlainText(file)
}

/** True if the file opens as a book in [format]. Plain text is accepted as long as it can be read. */
fun isReadable(file: File, format: String): Boolean = try {
    when (format) {
        "pdf" -> PdfPages(file).use { it.pageCount > 0 }
        "epub" -> ZipFile(file).use { it.getEntry("META-INF/container.xml") != null }
        else -> file.canRead()
    }
} catch (e: Exception) {
    false
}

/**
 * Renders PDF pages one at a time to bitmaps. PdfRenderer allows only one page open at once,
 * so the calls are serialised.
 */
class PdfPages(file: File) : Closeable {
    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)

    val pageCount: Int get() = renderer.pageCount

    @Synchronized
    fun render(index: Int, widthPx: Int): Bitmap {
        renderer.openPage(index).use { page ->
            val height = (widthPx * page.height / page.width.toFloat()).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        }
    }

    @Synchronized
    override fun close() {
        renderer.close()
        descriptor.close()
    }
}

private fun readPlainText(file: File): List<String> =
    file.readText(Charsets.UTF_8).removePrefix("\uFEFF").lines().map { it.trim() }.filter { it.isNotEmpty() }

/** An EPUB is a zip file: find the package file, then read its chapters in spine order. */
private fun readEpub(file: File): List<String> = ZipFile(file).use { zip ->
    val container = zip.readEntry("META-INF/container.xml") ?: return@use emptyList()
    val opfPath = Regex("full-path\\s*=\\s*\"([^\"]+)\"").find(container)?.groupValues?.get(1)
        ?: return@use emptyList()
    val opf = zip.readEntry(opfPath) ?: return@use emptyList()
    val folder = opfPath.substringBeforeLast('/', "")
    spineHrefs(opf).flatMap { href ->
        val path = Uri.decode(if (folder.isEmpty()) href else "$folder/$href")
        val html = zip.readEntry(path) ?: return@flatMap emptyList<String>()
        htmlParagraphs(html)
    }
}

/** The chapter files of an EPUB package, in the order the book reads them. */
private fun spineHrefs(opf: String): List<String> {
    val parser = Xml.newPullParser()
    parser.setInput(StringReader(opf))
    val manifest = HashMap<String, String>()
    val spine = ArrayList<String>()
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG) {
            when (parser.name) {
                "item" -> {
                    val id = parser.getAttributeValue(null, "id")
                    val href = parser.getAttributeValue(null, "href")
                    if (id != null && href != null) manifest[id] = href
                }
                "itemref" -> parser.getAttributeValue(null, "idref")?.let { spine.add(it) }
            }
        }
        event = parser.next()
    }
    return spine.mapNotNull { manifest[it] }
}

/** Strips the markup from one chapter and returns its non-empty lines. */
private fun htmlParagraphs(html: String): List<String> =
    Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        .replace('\u00A0', ' ')
        .replace("\uFFFC", "")
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }

private fun ZipFile.readEntry(name: String): String? =
    getEntry(name)?.let { entry -> getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() } }
