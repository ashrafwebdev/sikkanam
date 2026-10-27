package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.Place
import com.ashrafwebdev.sikkanam.data.PlaceLevel
import com.ashrafwebdev.sikkanam.network.lastNetworkUpdate
import kotlinx.coroutines.launch

/**
 * The curated list of places, from states down to masjids. The developer publishes the list and the
 * app downloads it, so every user sees the same places. Users can suggest additions by email.
 */
@Composable
fun NetworkSection(vm: FinanceViewModel, query: String = "") {
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
        SectionHeader(title = stringResource(R.string.network_title))
        Text(
            if (updatedAt == 0L) stringResource(R.string.network_not_downloaded)
            else stringResource(R.string.network_updated, dateLabel(updatedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
            ) { Text(if (updating) stringResource(R.string.network_updating) else stringResource(R.string.network_update)) }
            TextButton(onClick = { showSuggest = true }) { Text(stringResource(R.string.network_suggest)) }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (states.isEmpty() && updatedAt != 0L) {
            Text(stringResource(R.string.network_empty_list), style = MaterialTheme.typography.bodySmall)
        }
        val needle = query.trim()
        if (needle.isNotEmpty()) {
            // Searching shows every matching place as a flat list, with its path, instead of the tree.
            val matches = places.filter { it.name.contains(needle, ignoreCase = true) }
            if (matches.isEmpty()) {
                Text(stringResource(R.string.network_no_match), style = MaterialTheme.typography.bodySmall)
            }
            matches.forEach { place ->
                HorizontalDivider()
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(place.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${levelName(place.level)} · ${pathOf(place, places)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            states.forEach { state ->
                HorizontalDivider()
                PlaceNode(
                    place = state,
                    all = places,
                    depth = 0,
                    expanded = expanded,
                    onToggle = { id -> expanded = if (id in expanded) expanded - id else expanded + id },
                )
            }
        }
    }

    if (showSuggest) {
        SuggestPlaceDialog(onDismiss = { showSuggest = false })
    }
}

@Composable
private fun levelName(level: PlaceLevel): String = stringResource(
    when (level) {
        PlaceLevel.STATE -> R.string.network_level_state
        PlaceLevel.DISTRICT -> R.string.network_level_district
        PlaceLevel.ZONE -> R.string.network_level_zone
        PlaceLevel.HALQA -> R.string.network_level_halqa
        PlaceLevel.MASJID -> R.string.network_level_masjid
    },
)

@Composable
private fun childName(level: PlaceLevel): String = stringResource(
    when (level) {
        PlaceLevel.STATE -> R.string.network_plural_district
        PlaceLevel.DISTRICT -> R.string.network_plural_zone
        PlaceLevel.ZONE -> R.string.network_plural_halqa
        PlaceLevel.HALQA, PlaceLevel.MASJID -> R.string.network_plural_masjid
    },
)

/** The chain of names above a place, for example "Tamil Nadu › Chennai › Chennai › Guduvancheri". */
private fun pathOf(place: Place, all: List<Place>): String {
    val names = mutableListOf<String>()
    var current: Place? = place.parentId?.let { id -> all.firstOrNull { it.id == id } }
    while (current != null) {
        names.add(0, current.name)
        val parentId = current.parentId
        current = parentId?.let { id -> all.firstOrNull { it.id == id } }
    }
    return if (names.isEmpty()) "" else names.joinToString(" › ")
}

/** One place in the tree. Places with children open and close when tapped. */
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
    val isMasjid = place.level == PlaceLevel.MASJID
    val isTopLevel = depth == 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp)
            .clickable(enabled = children.isNotEmpty()) { onToggle(place.id) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            isMasjid -> Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            children.isNotEmpty() -> Icon(
                if (isOpen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = stringResource(if (isOpen) R.string.network_close else R.string.network_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            else -> Spacer(Modifier.width(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                place.name,
                style = if (isTopLevel) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isMasjid) FontWeight.Normal else FontWeight.SemiBold,
            )
            val info = if (children.isEmpty()) levelName(place.level)
            else stringResource(R.string.network_children, levelName(place.level), children.size, childName(place.level))
            Text(
                info,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    val suggestError = stringResource(R.string.network_suggest_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.network_suggest_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.network_suggest_desc),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text(stringResource(R.string.network_state)) }, singleLine = true)
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text(stringResource(R.string.network_district)) }, singleLine = true)
                OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text(stringResource(R.string.network_zone)) }, singleLine = true)
                OutlinedTextField(value = halqa, onValueChange = { halqa = it }, label = { Text(stringResource(R.string.network_halqa)) }, singleLine = true)
                OutlinedTextField(value = masjid, onValueChange = { masjid = it }, label = { Text(stringResource(R.string.network_masjid)) }, singleLine = true)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.network_note)) },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (state.isBlank() && district.isBlank() && masjid.isBlank()) {
                    error = suggestError
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
            }) { Text(stringResource(R.string.feedback_open_email)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
