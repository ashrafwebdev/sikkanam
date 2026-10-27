package com.ashrafwebdev.sikkanam.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.BuildConfig

private const val SUPPORT_EMAIL = "support@smatechsolution.com"

enum class FeedbackType(val label: String) {
    SUGGESTION("Suggestion"),
    FEEDBACK("Feedback"),
    MISTAKE("Report a mistake"),
}

/**
 * Settings card that opens the feedback form. The form opens the phone's own email app with the
 * message filled in. Nothing is sent by the app itself.
 */
@Composable
fun FeedbackSection() {
    var showForm by remember { mutableStateOf(false) }

    SectionCard {
        SectionHeader(title = "Feedback and support")
        Text(
            "Send a suggestion, feedback, or report a mistake. It opens your email app, so you can read it before sending.",
            style = MaterialTheme.typography.bodySmall,
        )
        FilledTonalButton(onClick = { showForm = true }) { Text("Send feedback") }
    }

    if (showForm) {
        FeedbackDialog(onDismiss = { showForm = false })
    }
}

@Composable
private fun FeedbackDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(FeedbackType.SUGGESTION) }
    var message by remember { mutableStateOf("") }
    var includeDevice by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send feedback") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeedbackType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        message = it
                        error = null
                    },
                    label = { Text("Your message") },
                    minLines = 4,
                    maxLines = 8,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeDevice, onCheckedChange = { includeDevice = it })
                    Text("Include app version and phone model", style = MaterialTheme.typography.bodySmall)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (message.isBlank()) {
                    error = "Please write your message first."
                } else {
                    if (openFeedbackEmail(context, type, message.trim(), includeDevice)) onDismiss()
                }
            }) { Text("Open email") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Opens the email app with the subject and body filled in. Returns false if no email app is installed. */
fun openSupportEmail(context: Context, subject: String, body: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No email app found. Please write to $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
        false
    }
}

/** Device details that help a developer reproduce a problem. */
fun deviceDetails(): String = buildString {
    appendLine("---")
    appendLine("App: Sikkanam ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}, build ${BuildConfig.VERSION_CODE})")
    appendLine("Phone: ${Build.MANUFACTURER} ${Build.MODEL}")
    appendLine("Android: ${Build.VERSION.RELEASE}")
}

private fun openFeedbackEmail(context: Context, type: FeedbackType, message: String, includeDevice: Boolean): Boolean {
    val body = buildString {
        appendLine(message)
        if (includeDevice) {
            appendLine()
            append(deviceDetails())
        }
    }
    return openSupportEmail(context, "[Sikkanam] ${type.label}", body)
}
