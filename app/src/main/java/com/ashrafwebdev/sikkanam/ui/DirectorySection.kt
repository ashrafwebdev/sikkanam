package com.ashrafwebdev.sikkanam.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.network.DirectoryEntry
import com.ashrafwebdev.sikkanam.network.loadDirectory
import com.ashrafwebdev.sikkanam.network.refreshDirectory
import kotlinx.coroutines.launch

/**
 * Service directory: electricians, plumbers, housekeepers, businesses. The developer publishes the
 * list and the app downloads it. Tapping a phone number opens the dialer; nothing is called automatically.
 * People can send their own details, or a change to a listing, by email.
 */
@Composable
fun DirectorySection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf(loadDirectory(context)) }
    var status by remember { mutableStateOf<String?>(null) }
    var updating by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var changeOf by remember { mutableStateOf<DirectoryEntry?>(null) }

    SectionCard {
        SectionHeader(title = "Services directory")
        Text(
            "Electricians, plumbers, housekeepers and businesses. Tap a phone number to call.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                enabled = !updating,
                onClick = {
                    updating = true
                    status = null
                    scope.launch {
                        status = refreshDirectory(context)
                        categories = loadDirectory(context)
                        updating = false
                    }
                },
            ) { Text(if (updating) "Updating..." else "Update directory") }
            OutlinedButton(onClick = { showForm = true }) { Text("Add my listing") }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (categories.isEmpty()) {
            Text("No listings yet. Tap Update directory (needs internet).", style = MaterialTheme.typography.bodySmall)
        }
        categories.forEach { category ->
            Text(category.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            category.entries.forEach { entry ->
                DirectoryRow(entry, onReportChange = { changeOf = entry })
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

@Composable
private fun DirectoryRow(entry: DirectoryEntry, onReportChange: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(entry.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        val line = listOfNotNull(
            entry.area.takeIf { it.isNotBlank() },
            entry.languages.joinToString(", ").takeIf { it.isNotBlank() }?.let { "Speaks $it" },
        ).joinToString(" · ")
        if (line.isNotBlank()) Text(line, style = MaterialTheme.typography.bodySmall)
        if (entry.description.isNotBlank()) Text(entry.description, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entry.phone.isNotBlank()) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${entry.phone}")))
                }) { Text("Call ${entry.phone}") }
            }
            TextButton(onClick = onReportChange) { Text("Report a change") }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (prefill == null) "Add my listing" else "Report a change") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Your details will be emailed to the developer, who adds them to the directory. " +
                        "Once listed, your name, phone number and description are shown to people using the app. " +
                        "Only send details you agree to share.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (electrician, plumber, housekeeper, business)") }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text("Area") }, singleLine = true)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                )
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Short description") }, minLines = 2)
                OutlinedTextField(value = languages, onValueChange = { languages = it }, label = { Text("Languages spoken (comma separated)") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || phone.isBlank()) {
                    error = "Please enter at least the name and phone number."
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
            }) { Text("Open email") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
