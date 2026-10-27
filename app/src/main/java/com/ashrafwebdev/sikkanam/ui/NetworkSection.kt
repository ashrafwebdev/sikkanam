package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.Place
import com.ashrafwebdev.sikkanam.network.lastNetworkUpdate
import kotlinx.coroutines.launch

/**
 * The curated list of places, from states down to masjids. The developer publishes the list and the
 * app downloads it, so every user sees the same places. Users can suggest additions by email.
 */
@Composable
fun NetworkSection(vm: FinanceViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val places by vm.places.collectAsState()
    var expanded by remember { mutableStateOf(setOf<Long>()) }
    var status by remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    var showSuggest by remember { mutableStateOf(false) }
    var updatedAt by remember { mutableStateOf(lastNetworkUpdate(context)) }

    val states = places.filter { it.parentId == null }

    SectionCard {
        SectionHeader(title = "Network (India)")
        Text(
            if (updatedAt == 0L) "The list has not been downloaded yet. Tap Update list (needs internet)."
            else "Last updated ${dateLabel(updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                enabled = !updating,
                onClick = {
                    updating = true
                    status = null
                    scope.launch {
                        status = vm.refreshNetwork()
                        updatedAt = lastNetworkUpdate(context)
                        updating = false
                    }
                },
            ) { Text(if (updating) "Updating..." else "Update list") }
            TextButton(onClick = { showSuggest = true }) { Text("Suggest a place") }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (states.isEmpty() && updatedAt != 0L) {
            Text("The downloaded list is empty.", style = MaterialTheme.typography.bodySmall)
        }
        states.forEach { state ->
            PlaceNode(
                place = state,
                all = places,
                depth = 0,
                expanded = expanded,
                onToggle = { id -> expanded = if (id in expanded) expanded - id else expanded + id },
            )
        }
    }

    if (showSuggest) {
        SuggestPlaceDialog(onDismiss = { showSuggest = false })
    }
}

/** One place in the tree, with its children shown below it when open. Read-only. */
@Composable
private fun PlaceNode(
    place: Place,
    all: List<Place>,
    depth: Int,
    expanded: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    val children = all.filter { it.parentId == place.id }
    val isOpen = place.id in expanded

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = (depth * 16).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (children.isNotEmpty()) {
            IconButton(onClick = { onToggle(place.id) }) {
                Icon(
                    if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isOpen) "Close" else "Open",
                )
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(place.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(place.level.label, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (isOpen) {
        children.forEach { child ->
            PlaceNode(child, all, depth + 1, expanded, onToggle)
        }
    }
}

/** Collects a place the user knows about. It is sent by the user's email app, and the user reads it first. */
@Composable
private fun SuggestPlaceDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("") }
    var halqa by remember { mutableStateOf("") }
    var masjid by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suggest a place") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Fill in what you know. Your email app opens with this, and you can read it before sending.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, singleLine = true)
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") }, singleLine = true)
                OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text("Zone") }, singleLine = true)
                OutlinedTextField(value = halqa, onValueChange = { halqa = it }, label = { Text("Halqa") }, singleLine = true)
                OutlinedTextField(value = masjid, onValueChange = { masjid = it }, label = { Text("Masjid") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (state.isBlank() && district.isBlank() && masjid.isBlank()) {
                    error = "Please fill in at least the state, district or masjid."
                } else {
                    val body = buildString {
                        appendLine("State: ${state.trim()}")
                        appendLine("District: ${district.trim()}")
                        appendLine("Zone: ${zone.trim()}")
                        appendLine("Halqa: ${halqa.trim()}")
                        appendLine("Masjid: ${masjid.trim()}")
                        if (note.isNotBlank()) appendLine("Note: ${note.trim()}")
                        appendLine()
                        append(deviceDetails())
                    }
                    if (openSupportEmail(context, "[Sikkanam] Network suggestion", body)) onDismiss()
                }
            }) { Text("Open email") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
