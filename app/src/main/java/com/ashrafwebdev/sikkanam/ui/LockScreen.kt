package com.ashrafwebdev.sikkanam.ui

import androidx.compose.ui.res.stringResource
import com.ashrafwebdev.sikkanam.R

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val PREFS = "sikkanam_prefs"
private const val KEY_LOCK = "lock_enabled"

/** Whether the user has turned on the app lock in Settings. */
fun isLockEnabled(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_LOCK, false)

fun setLockEnabled(context: Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_LOCK, enabled).apply()
}

/** The lock only works when the device has a fingerprint or screen-lock PIN set up. */
fun canUseLock(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity

    // Ask straight away, so the user doesn't have to tap first.
    LaunchedEffect(Unit) {
        authenticate(activity, onUnlock)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.lock_hint), style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = { authenticate(activity, onUnlock) },
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.lock_unlock))
        }
    }
}

private fun authenticate(activity: FragmentActivity, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.lock_prompt_title))
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()
    prompt.authenticate(info)
}
