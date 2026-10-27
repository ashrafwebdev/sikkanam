package com.ashrafwebdev.sikkanam.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ashrafwebdev.sikkanam.R

private const val PREFS = "backup_reminder"
private const val KEY_LAST_BACKUP = "last_backup_millis"
private const val MONTH_MILLIS = 30L * 24 * 60 * 60 * 1000

/** Remembers when the user last saved a backup. Call [markBackupSaved] after a backup is written. */
fun markBackupSaved(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
}

fun lastBackupMillis(context: Context): Long =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_BACKUP, 0L)

/** Shown on Home when there has been no backup for a month, or none has ever been saved. Hidden otherwise. */
@Composable
fun BackupReminderCard() {
    val context = LocalContext.current
    val last = lastBackupMillis(context)
    if (last != 0L && System.currentTimeMillis() - last < MONTH_MILLIS) return

    SectionCard {
        SectionHeader(title = stringResource(R.string.backup_reminder_title))
        Text(
            if (last == 0L) stringResource(R.string.backup_reminder_never)
            else stringResource(R.string.backup_reminder_last, dateLabel(last)),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(stringResource(R.string.backup_reminder_how), style = MaterialTheme.typography.bodySmall)
    }
}
