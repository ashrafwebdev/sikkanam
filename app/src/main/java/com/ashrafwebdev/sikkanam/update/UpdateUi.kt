package com.ashrafwebdev.sikkanam.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The newest build found by the last check, and the state of a download, shared by the banner and Settings. */
object UpdateStatus {
    private val _available = MutableStateFlow<AvailableUpdate?>(null)
    val available: StateFlow<AvailableUpdate?> = _available.asStateFlow()

    // Download progress from 0 to 1, or null when no download is running.
    private val _progress = MutableStateFlow<Float?>(null)
    val progress: StateFlow<Float?> = _progress.asStateFlow()

    // The last message about the install (for example, why it failed), or null.
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun set(update: AvailableUpdate?) {
        _available.value = update
    }

    fun setProgress(value: Float?) {
        _progress.value = value
    }

    fun setMessage(text: String?) {
        _message.value = text
    }
}

/** Opens the screen where the user allows this app to install other apps. */
private fun openInstallPermission(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
    context.startActivity(intent)
}

/** Shows a banner on every screen when a newer build is available, with progress while it downloads. */
@Composable
fun UpdateBanner(context: Context) {
    val update by UpdateStatus.available.collectAsState()
    val progress by UpdateStatus.progress.collectAsState()
    val message by UpdateStatus.message.collectAsState()
    val pending = update ?: return
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Update available: build ${pending.versionCode}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                enabled = progress == null,
                onClick = {
                    if (!canInstallUpdates(context)) {
                        // Android needs the user to allow installs from this app first.
                        UpdateStatus.setMessage("Allow Sikkanam to install apps, then tap Install again.")
                        openInstallPermission(context)
                        return@FilledTonalButton
                    }
                    UpdateStatus.setMessage(null)
                    UpdateStatus.setProgress(0f)
                    scope.launch {
                        val error = downloadAndInstall(context, pending) { UpdateStatus.setProgress(it) }
                        UpdateStatus.setProgress(null)
                        UpdateStatus.setMessage(error)
                    }
                },
            ) { Text("Install") }
        }
        progress?.let { value ->
            LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth())
            Text(
                "Downloading update… ${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
