package com.ashrafwebdev.sikkanam.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS = "ibadah_daily"

/** Today's counts are stored under a key with today's date, so each day starts from zero. */
private fun dayKey(prefix: String): String = prefix + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

private fun readCount(context: Context, prefix: String): Int =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(dayKey(prefix), 0)

private fun writeCount(context: Context, prefix: String, value: Int) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(dayKey(prefix), value.coerceAtLeast(0)).apply()
}

/**
 * A simple daily tracker: a dhikr counter and the number of Quran pages read today. Optional, and kept
 * on the phone only. Counts reset each day.
 */
@Composable
fun IbadahCard() {
    val context = LocalContext.current
    var dhikr by remember { mutableStateOf(readCount(context, "dhikr_")) }
    var pages by remember { mutableStateOf(readCount(context, "pages_")) }

    SectionCard {
        SectionHeader(title = stringResource(R.string.ibadah_title))
        Text(stringResource(R.string.ibadah_dhikr, dhikr), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                dhikr += 1
                writeCount(context, "dhikr_", dhikr)
            }) { Text(stringResource(R.string.ibadah_plus_one)) }
            OutlinedButton(onClick = {
                dhikr += 33
                writeCount(context, "dhikr_", dhikr)
            }) { Text(stringResource(R.string.ibadah_plus_33)) }
            OutlinedButton(onClick = {
                dhikr = 0
                writeCount(context, "dhikr_", dhikr)
            }) { Text(stringResource(R.string.ibadah_reset)) }
        }
        Text(stringResource(R.string.ibadah_pages, pages), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                pages += 1
                writeCount(context, "pages_", pages)
            }) { Text(stringResource(R.string.ibadah_plus_one)) }
            OutlinedButton(onClick = {
                pages = (pages - 1).coerceAtLeast(0)
                writeCount(context, "pages_", pages)
            }) { Text(stringResource(R.string.ibadah_minus_one)) }
        }
    }
}
