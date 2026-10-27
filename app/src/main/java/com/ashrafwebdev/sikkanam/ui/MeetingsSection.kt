package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.Meeting

/** Daily meetings (mashwara): a short log of each consultation, with who attended and what was decided. */
@Composable
fun MeetingsSection(vm: FinanceViewModel) {
    val meetings by vm.meetings.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Meeting?>(null) }

    SectionCard {
        SectionHeader(
            title = "Daily meetings",
            action = {
                FilledTonalButton(onClick = { showNew = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            },
        )
        if (meetings.isEmpty()) {
            Text("No meetings recorded yet.", style = MaterialTheme.typography.bodySmall)
        }
        // Show the latest few, so the list stays short as data grows.
        meetings.take(5).forEach { meeting ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(meeting.topic, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val line = listOf(dateLabel(meeting.dateMillis), meeting.attendees).filter { it.isNotBlank() }
                    Text(line.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { editing = meeting }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit meeting")
                }
                IconButton(onClick = { vm.deleteMeeting(meeting.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete meeting")
                }
            }
        }
        if (meetings.size > 5) {
            Text("Showing the latest 5 of ${meetings.size}.", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showNew) {
        MeetingDialog(
            existing = null,
            onDismiss = { showNew = false },
            onSave = { topic, attendees, notes ->
                if (vm.saveMeeting(null, topic, attendees, notes)) {
                    showNew = false
                    true
                } else {
                    false
                }
            },
        )
    }

    editing?.let { meeting ->
        MeetingDialog(
            existing = meeting,
            onDismiss = { editing = null },
            onSave = { topic, attendees, notes ->
                if (vm.saveMeeting(meeting, topic, attendees, notes)) {
                    editing = null
                    true
                } else {
                    false
                }
            },
        )
    }
}

@Composable
private fun MeetingDialog(
    existing: Meeting?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Boolean,
) {
    var topic by remember { mutableStateOf(existing?.topic ?: "") }
    var attendees by remember { mutableStateOf(existing?.attendees ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add daily meeting" else "Edit daily meeting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic") }, singleLine = true)
                OutlinedTextField(
                    value = attendees,
                    onValueChange = { attendees = it },
                    label = { Text("Who attended (names, separated by commas)") },
                    maxLines = 3,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes and decisions") },
                    maxLines = 6,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(topic, attendees, notes)) error = "Enter a topic." }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
