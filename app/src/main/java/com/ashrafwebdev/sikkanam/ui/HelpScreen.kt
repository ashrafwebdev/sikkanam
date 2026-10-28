package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R

/** Pairs of (title, text) for each part of the app, in the order the bottom bar shows them. */
private val helpSections: List<Pair<Int, Int>> = listOf(
    R.string.help_money_title to R.string.help_money_text,
    R.string.help_accounts_title to R.string.help_accounts_text,
    R.string.help_budgets_title to R.string.help_budgets_text,
    R.string.help_goals_title to R.string.help_goals_text,
    R.string.help_summary_title to R.string.help_summary_text,
    R.string.help_islamic_title to R.string.help_islamic_text,
    R.string.help_books_title to R.string.help_books_text,
    R.string.help_zakat_title to R.string.help_zakat_text,
    R.string.help_charity_title to R.string.help_charity_text,
    R.string.help_learn_title to R.string.help_learn_text,
    R.string.help_jamaat_title to R.string.help_jamaat_text,
    R.string.help_dawat_title to R.string.help_dawat_text,
    R.string.help_directory_title to R.string.help_directory_text,
    R.string.help_travel_title to R.string.help_travel_text,
    R.string.help_notes_title to R.string.help_notes_text,
    R.string.help_settings_title to R.string.help_settings_text,
    R.string.help_backup_title to R.string.help_backup_text,
)

/** The Help page: what each part of the app does and how to use it. */
@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionCard {
                Text(stringResource(R.string.help_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.help_intro), style = MaterialTheme.typography.bodySmall)
            }
        }
        items(helpSections) { (title, text) ->
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(text), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
