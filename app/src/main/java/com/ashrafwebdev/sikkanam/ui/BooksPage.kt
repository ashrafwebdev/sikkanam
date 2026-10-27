package com.ashrafwebdev.sikkanam.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.books.LocalBook
import com.ashrafwebdev.sikkanam.books.ReadingProgress
import com.ashrafwebdev.sikkanam.books.RemoteBook
import com.ashrafwebdev.sikkanam.books.downloadBook
import com.ashrafwebdev.sikkanam.books.importBook
import com.ashrafwebdev.sikkanam.books.loadLocalBooks
import com.ashrafwebdev.sikkanam.books.loadRemoteIndex
import com.ashrafwebdev.sikkanam.books.refreshRemoteIndex
import com.ashrafwebdev.sikkanam.books.removeBook
import kotlinx.coroutines.launch

/** What the import picker shows. Some file managers report an EPUB as a generic file, so that is included. */
private val BOOK_MIME_TYPES = arrayOf("application/epub+zip", "application/pdf", "text/plain", "application/octet-stream")

/**
 * Books page, inside the Islamic tab. It lists the books published on smatechsolution.com, the books
 * already on this phone, and an import button for any EPUB, PDF or TXT file. Tapping Read opens the
 * reader in place of the list.
 */
@Composable
fun BooksPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var remote by remember { mutableStateOf(loadRemoteIndex(context)) }
    var local by remember { mutableStateOf(loadLocalBooks(context)) }
    var readingId by rememberSaveable { mutableStateOf<String?>(null) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    var statusRes by remember { mutableStateOf<Int?>(null) }
    var removing by remember { mutableStateOf<LocalBook?>(null) }

    val pickBook = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                statusRes = null
                val book = importBook(context, uri)
                if (book == null) {
                    statusRes = R.string.books_import_failed
                } else {
                    local = loadLocalBooks(context)
                }
            }
        }
    }

    val reading = readingId?.let { id -> local.firstOrNull { it.id == id } }
    if (reading != null) {
        BookReaderScreen(reading, onBack = { readingId = null }, modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionCard {
                    SectionHeader(title = stringResource(R.string.books_title))
                    Text(
                        stringResource(R.string.books_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { pickBook.launch(BOOK_MIME_TYPES) }) {
                            Text(stringResource(R.string.books_import))
                        }
                        OutlinedButton(
                            enabled = !updating,
                            onClick = {
                                updating = true
                                statusRes = null
                                scope.launch {
                                    val ok = refreshRemoteIndex(context)
                                    remote = loadRemoteIndex(context)
                                    statusRes = if (ok) null else R.string.books_update_failed
                                    updating = false
                                }
                            },
                        ) {
                            Text(stringResource(if (updating) R.string.books_updating else R.string.books_update))
                        }
                    }
                    statusRes?.let {
                        Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                SectionCard {
                    SectionHeader(title = stringResource(R.string.books_on_phone))
                    if (local.isEmpty()) {
                        Text(
                            stringResource(R.string.books_none_on_phone),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    local.forEach { book ->
                        LocalBookRow(
                            book = book,
                            onRead = { readingId = book.id },
                            onRemove = { removing = book },
                        )
                    }
                }
            }

            item {
                SectionCard {
                    SectionHeader(title = stringResource(R.string.books_remote_title))
                    if (remote.isEmpty()) {
                        Text(
                            stringResource(R.string.books_remote_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    remote.forEach { book ->
                        RemoteBookRow(
                            book = book,
                            downloaded = local.any { it.id == book.id },
                            downloading = downloadingId == book.id,
                            downloadEnabled = downloadingId == null,
                            onRead = { readingId = book.id },
                            onDownload = {
                                downloadingId = book.id
                                statusRes = null
                                scope.launch {
                                    val saved = downloadBook(context, book)
                                    downloadingId = null
                                    if (saved == null) {
                                        statusRes = R.string.books_download_failed
                                    } else {
                                        local = loadLocalBooks(context)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    removing?.let { book ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.books_remove_title)) },
            text = { Text(stringResource(R.string.books_remove_text)) },
            confirmButton = {
                TextButton(onClick = {
                    removeBook(context, book)
                    local = loadLocalBooks(context)
                    removing = null
                }) { Text(stringResource(R.string.books_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(R.string.books_cancel)) }
            },
        )
    }
}

@Composable
private fun LocalBookRow(book: LocalBook, onRead: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val started = ReadingProgress.load(context, book.id) > 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            bookSubtitle(book.format, book.author),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onRead) {
                Text(stringResource(if (started) R.string.books_continue else R.string.books_read))
            }
            OutlinedButton(onClick = onRemove) { Text(stringResource(R.string.books_remove)) }
        }
    }
}

@Composable
private fun RemoteBookRow(
    book: RemoteBook,
    downloaded: Boolean,
    downloading: Boolean,
    downloadEnabled: Boolean,
    onRead: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            bookSubtitle(book.format, book.author),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (book.description.isNotBlank()) {
            Text(book.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
        }
        if (downloaded) {
            FilledTonalButton(onClick = onRead) { Text(stringResource(R.string.books_read)) }
        } else {
            FilledTonalButton(enabled = downloadEnabled, onClick = onDownload) {
                Text(stringResource(if (downloading) R.string.books_downloading else R.string.books_download))
            }
        }
    }
}

@Composable
private fun bookSubtitle(format: String, author: String): String {
    val parts = mutableListOf(format.uppercase())
    if (author.isNotBlank()) parts += stringResource(R.string.books_by, author)
    return parts.joinToString(" · ")
}
