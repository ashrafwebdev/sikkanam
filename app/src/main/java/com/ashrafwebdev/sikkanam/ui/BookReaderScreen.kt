package com.ashrafwebdev.sikkanam.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.books.BookText
import com.ashrafwebdev.sikkanam.books.LocalBook
import com.ashrafwebdev.sikkanam.books.PdfPages
import com.ashrafwebdev.sikkanam.books.ReaderFontSize
import com.ashrafwebdev.sikkanam.books.ReaderTheme
import com.ashrafwebdev.sikkanam.books.ReadingProgress
import com.ashrafwebdev.sikkanam.books.bookFile
import com.ashrafwebdev.sikkanam.books.readTextBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    var pdfZoom by remember { mutableStateOf(PDF_ZOOM_MIN) }
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
            if (book.format == "pdf") {
                TextButton(
                    onClick = { pdfZoom = (pdfZoom - PDF_ZOOM_STEP).coerceAtLeast(PDF_ZOOM_MIN) },
                    enabled = pdfZoom > PDF_ZOOM_MIN,
                ) { Text("−") }
                TextButton(
                    onClick = { pdfZoom = (pdfZoom + PDF_ZOOM_STEP).coerceAtMost(PDF_ZOOM_MAX) },
                    enabled = pdfZoom < PDF_ZOOM_MAX,
                ) { Text("+") }
            } else {
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
            book.format == "pdf" -> PdfBook(book.id, file, pdfZoom, { pdfZoom = it }, Modifier.weight(1f))
            else -> TextBook(book.id, book.format, file, fontStep, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TextBook(bookId: String, format: String, file: File, fontStep: Int, modifier: Modifier) {
    val loaded by produceState<Result<BookText>?>(null, file, format) {
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
private fun ParagraphList(bookId: String, text: BookText, fontStep: Int, modifier: Modifier) {
    if (text.lines.isEmpty()) {
        Message(stringResource(R.string.books_empty_book), modifier)
        return
    }
    val context = LocalContext.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = ReadingProgress.load(context, bookId))
    SaveProgress(bookId, listState)
    val scope = rememberCoroutineScope()
    val headingLines = remember(text) { text.headings.toHashSet() }
    var theme by remember { mutableStateOf(ReaderTheme.load(context)) }
    var showContents by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    val (background, foreground) = readerColors(theme)
    // Step 1 is the normal 16 sp body text; each step up or down adds or removes 2 sp.
    val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = (14 + 2 * fontStep).sp)
    Column(modifier = modifier.fillMaxWidth().background(background).padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (text.headings.isNotEmpty()) {
                TextButton(onClick = { showContents = true }) { Text(stringResource(R.string.reader_contents)) }
            }
            TextButton(onClick = { showSearch = true }) { Text(stringResource(R.string.reader_search)) }
            TextButton(onClick = {
                theme = ReaderTheme.next(theme)
                ReaderTheme.save(context, theme)
            }) { Text(stringResource(themeName(theme))) }
        }
        BookmarkBar(bookId, listState)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(text.lines.size) { index ->
                Text(
                    text.lines[index],
                    style = if (index in headingLines) textStyle.copy(fontWeight = FontWeight.Bold) else textStyle,
                    color = foreground,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
    if (showContents) {
        ContentsDialog(
            lines = text.lines,
            headings = text.headings,
            onDismiss = { showContents = false },
            onPick = { index ->
                showContents = false
                scope.launch { listState.scrollToItem(index) }
            },
        )
    }
    if (showSearch) {
        SearchDialog(
            lines = text.lines,
            onDismiss = { showSearch = false },
            onPick = { index ->
                showSearch = false
                scope.launch { listState.scrollToItem(index) }
            },
        )
    }
}

/** Background and text colour for a reader theme. The light theme uses the app's own colours. */
@Composable
private fun readerColors(theme: Int): Pair<Color, Color> = when (theme) {
    ReaderTheme.SEPIA -> Color(0xFFF4ECD8) to Color(0xFF3B2F1E)
    ReaderTheme.DARK -> Color(0xFF1C1C1E) to Color(0xFFE6E6E6)
    else -> MaterialTheme.colorScheme.background to MaterialTheme.colorScheme.onBackground
}

private fun themeName(theme: Int): Int = when (theme) {
    ReaderTheme.SEPIA -> R.string.reader_theme_sepia
    ReaderTheme.DARK -> R.string.reader_theme_dark
    else -> R.string.reader_theme_light
}

/** The chapter and section titles of the book. Tapping one jumps to it. */
@Composable
private fun ContentsDialog(lines: List<String>, headings: List<Int>, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_contents)) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                headings.forEach { index ->
                    Text(
                        lines[index],
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(index) }.padding(vertical = 10.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.reader_close)) } },
    )
}

/** Finds a word or phrase in the book. Tapping a result jumps to that paragraph. */
@Composable
private fun SearchDialog(lines: List<String>, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    val term = query.trim()
    val matches = if (term.isEmpty()) {
        emptyList<IndexedValue<String>>()
    } else {
        lines.withIndex().filter { it.value.contains(term, ignoreCase = true) }.take(MAX_SEARCH_RESULTS)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_search_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.reader_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (term.isNotEmpty() && matches.isEmpty()) {
                    Text(stringResource(R.string.reader_search_none), style = MaterialTheme.typography.bodySmall)
                }
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    matches.forEach { match ->
                        Text(
                            snippet(match.value, term),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(match.index) }.padding(vertical = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.reader_close)) } },
    )
}

/** A short piece of [line] around the first place [term] is found, so long paragraphs fit in the list. */
private fun snippet(line: String, term: String): String {
    if (line.length <= SNIPPET_LENGTH) return line
    val at = line.indexOf(term, ignoreCase = true).coerceAtLeast(0)
    val start = (at - SNIPPET_LENGTH / 3).coerceIn(0, line.length - SNIPPET_LENGTH)
    val end = start + SNIPPET_LENGTH
    return (if (start > 0) "…" else "") + line.substring(start, end) + (if (end < line.length) "…" else "")
}

private const val MAX_SEARCH_RESULTS = 200
private const val SNIPPET_LENGTH = 120

/** Marks the place on screen with a bookmark, and jumps back to saved bookmarks. */
@Composable
private fun BookmarkBar(bookId: String, listState: LazyListState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var marks by remember(bookId) { mutableStateOf(ReadingProgress.bookmarks(context, bookId)) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                ReadingProgress.addBookmark(context, bookId, listState.firstVisibleItemIndex)
                marks = ReadingProgress.bookmarks(context, bookId)
            }) { Text(stringResource(R.string.books_bookmark_here)) }
            if (marks.isNotEmpty()) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    ReadingProgress.clearBookmarks(context, bookId)
                    marks = emptyList()
                }) { Text(stringResource(R.string.books_bookmarks_clear)) }
            }
        }
        if (marks.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                marks.forEach { index ->
                    AssistChip(
                        onClick = { scope.launch { listState.scrollToItem(index) } },
                        label = { Text(stringResource(R.string.books_bookmark_place, index + 1)) },
                    )
                }
            }
        }
    }
}

/** How far a PDF page can be zoomed: from the width of the screen up to three times that. */
private const val PDF_ZOOM_MIN = 1f
private const val PDF_ZOOM_MAX = 3f
private const val PDF_ZOOM_STEP = 0.25f

@Composable
private fun PdfBook(bookId: String, file: File, zoom: Float, onZoom: (Float) -> Unit, modifier: Modifier) {
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
        else -> PdfPageList(bookId, pdf.getOrThrow(), zoom, onZoom, modifier)
    }
}

@Composable
private fun PdfPageList(bookId: String, pdf: PdfPages, zoom: Float, onZoom: (Float) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    // Pages are drawn to the width of the screen inside the side padding, times the zoom.
    val pageWidth = LocalConfiguration.current.screenWidthDp.dp - 32.dp
    val zoomedWidth = pageWidth * zoom
    val widthPx = with(density) { zoomedWidth.roundToPx() }.coerceAtLeast(100)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = ReadingProgress.load(context, bookId))
    SaveProgress(bookId, listState)
    // Pinch with two fingers to zoom. Zoomed pages that are wider than the screen scroll sideways.
    val currentZoom by rememberUpdatedState(zoom)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        BookmarkBar(bookId, listState)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        onZoom((currentZoom * zoomChange).coerceIn(PDF_ZOOM_MIN, PDF_ZOOM_MAX))
                    }
                }
                .horizontalScroll(rememberScrollState()),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(zoomedWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(count = pdf.pageCount) { index -> PdfPageImage(pdf, index, widthPx) }
            }
        }
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
    // Once the last item has been on screen, the book counts as read, so the library shows Read, not Continue.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { last ->
            if (last != null && last >= listState.layoutInfo.totalItemsCount - 1) {
                ReadingProgress.markFinished(context, bookId)
            }
        }
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
