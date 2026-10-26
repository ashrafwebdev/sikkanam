package com.ashrafwebdev.sikkanam

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.ashrafwebdev.sikkanam.ui.LockScreen
import com.ashrafwebdev.sikkanam.ui.SikkanamScreen
import com.ashrafwebdev.sikkanam.ui.canUseLock
import com.ashrafwebdev.sikkanam.ui.isLockEnabled

// FragmentActivity (instead of ComponentActivity) is needed for the biometric prompt.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val lockRequired = isLockEnabled(context) && canUseLock(context)
    var unlocked by rememberSaveable { mutableStateOf(false) }

    if (lockRequired && !unlocked) {
        LockScreen(onUnlock = { unlocked = true })
    } else {
        SikkanamScreen()
    }
}
