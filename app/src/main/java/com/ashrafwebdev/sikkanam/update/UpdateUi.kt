package com.ashrafwebdev.sikkanam.update

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The newest build found by the last check, shared between the banner and Settings. */
object UpdateStatus {
    private val _available = MutableStateFlow<AvailableUpdate?>(null)
    val available: StateFlow<AvailableUpdate?> = _available.asStateFlow()

    fun set(update: AvailableUpdate?) {
        _available.value = update
    }
}

/** Shows a banner on every screen when a newer build is available. */
@Composable
fun UpdateBanner(context: Context) {
    val update by UpdateStatus.available.collectAsState()
    val pending = update ?: return

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Update available: build ${pending.versionCode}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        val scope = rememberCoroutineScope()
        FilledTonalButton(onClick = {
            // Downloads the APK, then Android's install prompt takes over.
            scope.launch { downloadAndInstall(context, pending) }
        }) { Text("Install") }
    }
}
