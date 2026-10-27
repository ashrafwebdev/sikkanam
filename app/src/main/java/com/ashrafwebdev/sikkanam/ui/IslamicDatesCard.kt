package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.islamic.upcomingIslamicEvents

/** The next key Islamic dates, with how many days away each one is. Works offline. */
@Composable
fun IslamicDatesCard() {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val upcoming = upcomingIslamicEvents(now, 5)

    SectionCard {
        SectionHeader(title = stringResource(R.string.islamic_title))
        Text(
            stringResource(R.string.islamic_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            upcoming.forEach { item ->
                val days = ((item.millis - now) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
                val nameRes = context.resources.getIdentifier(item.event.nameKey, "string", context.packageName)
                val name = if (nameRes != 0) stringResource(nameRes) else item.event.nameKey
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.islamic_days_away, dateLabel(item.millis), days),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
