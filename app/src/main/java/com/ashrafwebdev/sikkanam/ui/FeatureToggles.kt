package com.ashrafwebdev.sikkanam.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ashrafwebdev.sikkanam.R

private const val PREFS = "display"
private const val KEY_ISLAMIC = "islamic_pages_on"
private const val KEY_JAMAAT = "jamaat_pages_on"

/** Whether the Islamic pages (prayer, zakat, charity, learning) are shown. On by default. */
fun islamicPagesOn(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ISLAMIC, true)

/** Whether the Jamaat pages (jamaat, dawat, locality, mashwara) are shown. On by default. */
fun jamaatPagesOn(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_JAMAAT, true)

fun setIslamicPagesOn(context: Context, on: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ISLAMIC, on).apply()
}

fun setJamaatPagesOn(context: Context, on: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_JAMAAT, on).apply()
}

/** Settings: switch the Islamic and Jamaat pages on or off. The app restarts its screen to apply the change. */
@Composable
fun PagesSection() {
    val context = LocalContext.current
    var islamic by remember { mutableStateOf(islamicPagesOn(context)) }
    var jamaat by remember { mutableStateOf(jamaatPagesOn(context)) }

    SectionCard {
        SectionHeader(title = stringResource(R.string.pages_title))
        Text(stringResource(R.string.pages_desc), style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.pages_islamic), style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = islamic,
                onCheckedChange = {
                    islamic = it
                    setIslamicPagesOn(context, it)
                    (context as? Activity)?.recreate()
                },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.pages_jamaat), style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = jamaat,
                onCheckedChange = {
                    jamaat = it
                    setJamaatPagesOn(context, it)
                    (context as? Activity)?.recreate()
                },
            )
        }
    }
}
