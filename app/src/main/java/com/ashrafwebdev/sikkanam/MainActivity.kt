package com.ashrafwebdev.sikkanam

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.ashrafwebdev.sikkanam.reminders.canNotify
import com.ashrafwebdev.sikkanam.widget.BalanceWidget
import com.ashrafwebdev.sikkanam.reminders.scheduleReminders
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
import com.ashrafwebdev.sikkanam.ui.SikkanamTheme
import com.ashrafwebdev.sikkanam.ui.isLockEnabled

// FragmentActivity (instead of ComponentActivity) is needed for the biometric prompt.
class MainActivity : FragmentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onPause() {
        super.onPause()
        // Refresh the home-screen widget so it shows the latest balance when the user leaves the app.
        lifecycleScope.launch { BalanceWidget().updateAll(this@MainActivity) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleReminders(this)
        if (!canNotify(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SikkanamTheme {
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
