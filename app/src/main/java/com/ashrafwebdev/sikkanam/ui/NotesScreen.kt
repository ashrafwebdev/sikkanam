package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.Note

/** Personal notes: each has a title and a text. The list can be searched by title or text. */
@Composable
fun NotesScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    val allNotes by vm.notes.collectAsState()
    var query by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }
    var viewing by remember { mutableStateOf<Note?>(null) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    val search = query.trim()
    val shown = allNotes.filter { note ->
        search.isEmpty() ||
            note.title.contains(search, ignoreCase = true) ||
            note.body.contains(search, ignoreCase = true) ||
            dateLabel(note.updatedMillis).contains(search)
    }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SectionCard {
                SectionHeader(
                    title = stringResource(R.string.notes_title),
                    action = {
                        FilledTonalIconButton(onClick = { showNew = true }) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
    }
                    },
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.notes_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (allNotes.isEmpty()) {
            item {
                SectionCard {
                    Text(stringResource(R.string.notes_empty), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (shown.isEmpty()) {
            item {
                SectionCard {
                    Text(stringResource(R.string.notes_no_match), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(shown) { note ->
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewing = note },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            note.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (note.pinned) {
                            Text(
                                stringResource(R.string.notes_pinned),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            note.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(R.string.notes_updated, dateLabel(note.updatedMillis)),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    IconButton(onClick = { editing = note }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.notes_edit_desc))
                    }
                    IconButton(onClick = { deleting = note }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.notes_delete_desc))
                    }
                }
            }
        }
    }

    if (showNew) {
        NoteDialog(
            existing = null,
            onDismiss = { showNew = false },
            onSave = { title, body ->
                if (vm.saveNote(null, title, body)) {
                    showNew = false
                    true
                } else {
                    false
                }
            },
        )
    }

    editing?.let { note ->
        NoteDialog(
            existing = note,
            onDismiss = { editing = null },
            onSave = { title, body ->
                if (vm.saveNote(note, title, body)) {
                    editing = null
                    true
                } else {
                    false
                }
            },
        )
    }

    viewing?.let { note ->
        NoteViewDialog(
            note = note,
            onDismiss = { viewing = null },
            onTogglePin = {
                vm.togglePinNote(note)
                viewing = null
            },
            onEdit = {
                viewing = null
                editing = note
            },
            onDelete = {
                viewing = null
                deleting = note
            },
        )
    }

    deleting?.let { note ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.notes_delete_confirm)) },
            text = { Text(note.title, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteNote(note.id)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Shows the whole note. Edit and Delete are reached from here or from the list. */
@Composable
private fun NoteViewDialog(
    note: Note,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.notes_updated, dateLabel(note.updatedMillis)),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(note.body, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onTogglePin) {
                    Text(stringResource(if (note.pinned) R.string.notes_unpin else R.string.notes_pin))
                }
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) } },
        dismissButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) } },
    )
}

@Composable
private fun NoteDialog(
    existing: Note?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Boolean,
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var body by remember { mutableStateOf(existing?.body ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val titleError = stringResource(R.string.notes_title_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) stringResource(R.string.notes_add_title)
                else stringResource(R.string.notes_edit_title),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.notes_field_title)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(stringResource(R.string.notes_field_body)) },
                    minLines = 5,
                    maxLines = 12,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(title, body)) error = titleError }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
