package com.ashrafwebdev.sikkanam.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.books.LocalBook
import com.ashrafwebdev.sikkanam.books.PdfPages
import com.ashrafwebdev.sikkanam.books.ReaderFontSize
import com.ashrafwebdev.sikkanam.books.ReadingProgress
import com.ashrafwebdev.sikkanam.books.bookFile
import com.ashrafwebdev.sikkanam.books.readTextBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads one book on the phone. PDFs show one page after another as pictures; EPUB and TXT files
 * show their text. The place is saved as the reader scrolls, so Continue reading returns to it.
 */
@Composable
fun BookReaderScreen(book: LocalBook, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val file = bookFile(context, book)
    var fontStep by remember { mutableStateOf(ReaderFontSize.load(context)) }
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.books_back))
            }
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (book.format != "pdf") {
                TextButton(
                    onClick = {
                        fontStep = (fontStep - 1).coerceAtLeast(ReaderFontSize.MIN_STEP)
                        ReaderFontSize.save(context, fontStep)
                    },
                    enabled = fontStep > ReaderFontSize.MIN_STEP,
                ) { Text("A-") }
                TextButton(
                    onClick = {
                        fontStep = (fontStep + 1).coerceAtMost(ReaderFontSize.MAX_STEP)
                        ReaderFontSize.save(context, fontStep)
                    },
                    enabled = fontStep < ReaderFontSize.MAX_STEP,
                ) { Text("A+") }
            }
        }
        when {
            !file.exists() -> Message(stringResource(R.string.books_missing), Modifier.weight(1f))
            book.format == "pdf" -> PdfBook(book.id, file, Modifier.weight(1f))
            else -> TextBook(book.id, book.format, file, fontStep, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TextBook(bookId: String, format: String, file: File, fontStep: Int, modifier: Modifier) {
    val loaded by produceState<Result<List<String>>?>(null, file, format) {
        value = withContext(Dispatchers.IO) { runCatching { readTextBook(file, format) } }
    }
    val result = loaded
    when {
        result == null -> Loading(modifier)
        result.isFailure -> Message(stringResource(R.string.books_error_open), modifier)
        else -> ParagraphList(bookId, result.getOrThrow(), fontStep, modifier)
    }
}

@Composable
private fun ParagraphList(bookId: String, paragraphs: List<String>, fontStep: Int, modifier: Modifier) {
    if (paragraphs.isEmpty()) {
        Message(stringResource(R.string.books_empty_book), modifier)
        return
    }
    val context = LocalContext.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = ReadingProgress.load(context, bookId))
    SaveProgress(bookId, listState)
    // Step 1 is the normal 16 sp body text; each step up or down adds or removes 2 sp.
    val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = (14 + 2 * fontStep).sp)
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(paragraphs) { paragraph ->
            Text(
                paragraph,
                style = textStyle,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun PdfBook(bookId: String, file: File, modifier: Modifier) {
    val opened by produceState<Result<PdfPages>?>(null, file) {
        value = withContext(Dispatchers.IO) { runCatching { PdfPages(file) } }
    }
    val pdf = opened
    DisposableEffect(pdf) {
        onDispose { pdf?.getOrNull()?.close() }
    }
    when {
        pdf == null -> Loading(modifier)
        pdf.isFailure -> Message(stringResource(R.string.books_error_open), modifier)
        else -> PdfPageList(bookId, pdf.getOrThrow(), modifier)
    }
}

@Composable
private fun PdfPageList(bookId: String, pdf: PdfPages, modifier: Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // Pages are drawn to the width of the screen inside the side padding.
    val widthPx = with(density) { (LocalConfiguration.current.screenWidthDp.dp - 32.dp).roundToPx() }
        .coerceAtLeast(100)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = ReadingProgress.load(context, bookId))
    SaveProgress(bookId, listState)
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(count = pdf.pageCount) { index -> PdfPageImage(pdf, index, widthPx) }
    }
}

@Composable
private fun PdfPageImage(pdf: PdfPages, index: Int, widthPx: Int) {
    val page by produceState<ImageBitmap?>(null, pdf, index, widthPx) {
        value = withContext(Dispatchers.IO) { pdf.render(index, widthPx).asImageBitmap() }
    }
    val image = page
    if (image == null) {
        Box(modifier = Modifier.fillMaxWidth().height(480.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Image(
            bitmap = image,
            contentDescription = stringResource(R.string.books_page_number, index + 1),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Saves the first visible item each time the reader scrolls. */
@Composable
private fun SaveProgress(bookId: String, listState: LazyListState) {
    val context = LocalContext.current
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { ReadingProgress.save(context, bookId, it) }
    }
}

@Composable
private fun Loading(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Message(text: String, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center)
    }
}
