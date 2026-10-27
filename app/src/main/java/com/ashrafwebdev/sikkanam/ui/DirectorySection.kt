package com.ashrafwebdev.sikkanam.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.ashrafwebdev.sikkanam.network.DirectoryCategory
import com.ashrafwebdev.sikkanam.network.DirectoryEntry
import com.ashrafwebdev.sikkanam.network.refreshDirectory
import kotlinx.coroutines.launch

/**
 * Services directory: electricians, plumbers, housekeepers, businesses. The developer publishes the
 * list and the app downloads it. Tapping Call opens the dialer; nothing is called automatically.
 * [query] and [categoryFilter] narrow the listings. A null filter shows every category.
 */
@Composable
fun DirectorySection(
    categories: List<DirectoryCategory>,
    query: String,
    categoryFilter: String?,
    onReload: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var changeOf by remember { mutableStateOf<DirectoryEntry?>(null) }

    val needle = query.trim()
    val visible = categories
        .filter { categoryFilter == null || it.name == categoryFilter }
        .map { category ->
            category.copy(entries = category.entries.filter { entry -> needle.isEmpty() || matches(entry, category, needle) })
        }
        .filter { it.entries.isNotEmpty() }

    SectionCard {
        SectionHeader(title = stringResource(R.string.directory_title))
        Text(
            stringResource(R.string.directory_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                enabled = !updating,
                onClick = {
                    updating = true
                    status = null
                    scope.launch {
                        status = refreshDirectory(context)
                        onReload()
                        updating = false
                    }
                },
            ) { Text(if (updating) stringResource(R.string.directory_updating) else stringResource(R.string.directory_update)) }
            OutlinedButton(onClick = { showForm = true }) { Text(stringResource(R.string.directory_add)) }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (categories.isEmpty()) {
            Text(
                stringResource(R.string.directory_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (visible.isEmpty()) {
            Text(stringResource(R.string.directory_no_match), style = MaterialTheme.typography.bodySmall)
        }
        visible.forEach { category ->
            CategoryHeader(category)
            category.entries.forEach { entry ->
                DirectoryCard(entry, onReportChange = { changeOf = entry })
            }
        }
    }

    if (showForm) {
        DirectoryFormDialog(prefill = null, onDismiss = { showForm = false })
    }
    changeOf?.let { entry ->
        DirectoryFormDialog(prefill = entry, onDismiss = { changeOf = null })
    }
}

/** A listing matches when the search text appears in its category, name, area, description or languages. */
private fun matches(entry: DirectoryEntry, category: DirectoryCategory, needle: String): Boolean =
    listOf(category.name, entry.name, entry.area, entry.description, entry.languages.joinToString(" "))
        .any { it.contains(needle, ignoreCase = true) }

@Composable
private fun CategoryHeader(category: DirectoryCategory) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            category.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "${category.entries.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One listing as a card: name, area and languages, a short description, then the actions. */
@Composable
private fun DirectoryCard(entry: DirectoryEntry, onReportChange: () -> Unit) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val meta = listOfNotNull(
                entry.area.takeIf { it.isNotBlank() },
                entry.languages.joinToString(", ").takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.directory_speaks, it) },
            ).joinToString("  ·  ")
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (entry.description.isNotBlank()) {
                Text(entry.description, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (entry.phone.isNotBlank()) {
                    FilledTonalButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entry.phone}")))
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text(entry.phone)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onReportChange) { Text(stringResource(R.string.directory_report)) }
            }
        }
    }
}

/**
 * Collects someone's details for the directory, or a change to an existing listing ([prefill]).
 * The details go by email to the developer, who reads them before adding anything.
 */
@Composable
private fun DirectoryFormDialog(prefill: DirectoryEntry?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var category by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(prefill?.name.orEmpty()) }
    var area by remember { mutableStateOf(prefill?.area.orEmpty()) }
    var phone by remember { mutableStateOf(prefill?.phone.orEmpty()) }
    var description by remember { mutableStateOf(prefill?.description.orEmpty()) }
    var languages by remember { mutableStateOf(prefill?.languages?.joinToString(", ").orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val formError = stringResource(R.string.directory_form_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (prefill == null) stringResource(R.string.directory_form_new_title)
                else stringResource(R.string.directory_form_change_title),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.directory_form_info),
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.directory_field_category)) },
                    singleLine = true,
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.directory_field_name)) }, singleLine = true)
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text(stringResource(R.string.directory_field_area)) }, singleLine = true)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.directory_field_phone)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.directory_field_description)) },
                    minLines = 2,
                )
                OutlinedTextField(
                    value = languages,
                    onValueChange = { languages = it },
                    label = { Text(stringResource(R.string.directory_field_languages)) },
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || phone.isBlank()) {
                    error = formError
                } else {
                    val body = buildString {
                        appendLine(if (prefill == null) "New listing request" else "Change to an existing listing")
                        appendLine("Category: ${category.trim()}")
                        appendLine("Name: ${name.trim()}")
                        appendLine("Area: ${area.trim()}")
                        appendLine("Phone: ${phone.trim()}")
                        appendLine("Description: ${description.trim()}")
                        appendLine("Languages: ${languages.trim()}")
                        appendLine()
                        append(deviceDetails())
                    }
                    val subject = if (prefill == null) "[Sikkanam] Directory listing" else "[Sikkanam] Directory change"
                    if (openSupportEmail(context, subject, body)) onDismiss()
                }
            }) { Text(stringResource(R.string.feedback_open_email)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
