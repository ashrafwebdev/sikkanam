package com.ashrafwebdev.sikkanam.ui
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh


import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

/** The personal daily amaal, ticked off each day. */
private enum class DailyAmaal(@StringRes val labelRes: Int) {
    QURAN(R.string.infiradi_quran),
    THIRD_KALIMAH(R.string.infiradi_third_kalimah),
    SALWAAT(R.string.infiradi_salwaat),
    ISTIGHFAR(R.string.infiradi_istighfar),
    JAMAAT_PRAYERS(R.string.infiradi_jamaat_prayers),
    TAHAJJUD(R.string.infiradi_tahajjud),
    ISHRAQ(R.string.infiradi_ishraq),
    LUHA(R.string.infiradi_luha),
    AWWABEEN(R.string.infiradi_awwabeen),
    HALAL_RIZQ(R.string.infiradi_halal_rizq),
}

/** A checklist of the personal daily amaal. Each day's ticks are kept separately, like the counters. */
@Composable
private fun DailyAmaalChecklist() {
    val context = LocalContext.current
    var ticked by remember {
        mutableStateOf(DailyAmaal.entries.filter { context.prefs().getBoolean(dayKey("amaal_${it.name}"), false) }.toSet())
    }

    SectionHeader(title = stringResource(R.string.infiradi_title))
    DailyAmaal.entries.forEach { amaal ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = amaal in ticked,
                onCheckedChange = { checked ->
                    context.prefs().edit().putBoolean(dayKey("amaal_${amaal.name}"), checked).apply()
                    ticked = if (checked) ticked + amaal else ticked - amaal
                },
            )
            Text(stringResource(amaal.labelRes), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun Context.prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

/**
 * The personal daily amaal checklist, plus a dhikr counter and the number of Quran pages read today.
 * Optional, and kept on the phone only. Everything resets each day.
 */
@Composable
fun IbadahCard() {
    val context = LocalContext.current
    var dhikr by remember { mutableStateOf(readCount(context, "dhikr_")) }
    var pages by remember { mutableStateOf(readCount(context, "pages_")) }

    SectionCard {
        SectionHeader(title = stringResource(R.string.ibadah_title))
        DailyAmaalChecklist()
        Text(stringResource(R.string.ibadah_dhikr, dhikr), style = MaterialTheme.typography.bodyLarge)
        ButtonRow {
            FilledTonalButton(onClick = {
                dhikr += 1
                writeCount(context, "dhikr_", dhikr)
            }) { Text(stringResource(R.string.ibadah_plus_one)) }
            OutlinedButton(onClick = {
                dhikr += 33
                writeCount(context, "dhikr_", dhikr)
            }) { Text(stringResource(R.string.ibadah_plus_33)) }
            OutlinedIconButton(onClick = {
                dhikr = 0
                writeCount(context, "dhikr_", dhikr)
            }) { Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.ibadah_reset)) }
        }
        Text(stringResource(R.string.ibadah_pages, pages), style = MaterialTheme.typography.bodyLarge)
        ButtonRow {
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
