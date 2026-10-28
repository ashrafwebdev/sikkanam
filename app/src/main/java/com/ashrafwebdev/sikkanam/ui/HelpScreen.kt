package com.ashrafwebdev.sikkanam.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R

/** The chips at the top of the Help page. Each one groups the help topics of one part of the app. */
private enum class HelpPart(@StringRes val labelRes: Int) {
    MONEY(R.string.tab_money),
    ISLAMIC(R.string.tab_islamic),
    JAMAAT(R.string.tab_jamaat),
    MORE(R.string.section_more),
}

/** Each help topic with its part, title and text, in the order the bottom bar shows them. */
private val helpSections: List<Triple<HelpPart, Int, Int>> = listOf(
    Triple(HelpPart.MONEY, R.string.help_money_title, R.string.help_money_text),
    Triple(HelpPart.MONEY, R.string.help_accounts_title, R.string.help_accounts_text),
    Triple(HelpPart.MONEY, R.string.help_budgets_title, R.string.help_budgets_text),
    Triple(HelpPart.MONEY, R.string.help_goals_title, R.string.help_goals_text),
    Triple(HelpPart.MONEY, R.string.help_summary_title, R.string.help_summary_text),
    Triple(HelpPart.ISLAMIC, R.string.help_islamic_title, R.string.help_islamic_text),
    Triple(HelpPart.ISLAMIC, R.string.help_books_title, R.string.help_books_text),
    Triple(HelpPart.ISLAMIC, R.string.help_zakat_title, R.string.help_zakat_text),
    Triple(HelpPart.ISLAMIC, R.string.help_charity_title, R.string.help_charity_text),
    Triple(HelpPart.ISLAMIC, R.string.help_learn_title, R.string.help_learn_text),
    Triple(HelpPart.JAMAAT, R.string.help_jamaat_title, R.string.help_jamaat_text),
    Triple(HelpPart.JAMAAT, R.string.help_dawat_title, R.string.help_dawat_text),
    Triple(HelpPart.JAMAAT, R.string.help_people_title, R.string.help_people_text),
    Triple(HelpPart.JAMAAT, R.string.help_mastoorat_title, R.string.help_mastoorat_text),
    Triple(HelpPart.JAMAAT, R.string.help_directory_title, R.string.help_directory_text),
    Triple(HelpPart.JAMAAT, R.string.help_travel_title, R.string.help_travel_text),
    Triple(HelpPart.MORE, R.string.help_notes_title, R.string.help_notes_text),
    Triple(HelpPart.MORE, R.string.help_settings_title, R.string.help_settings_text),
    Triple(HelpPart.MORE, R.string.help_backup_title, R.string.help_backup_text),
)

/** The Help page: what each part of the app does and how to use it. */
@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    var part by rememberSaveable { mutableStateOf(HelpPart.MONEY) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionChips(
            options = HelpPart.entries,
            selected = part,
            label = { stringResource(it.labelRes) },
            onSelect = { part = it },
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionCard {
                    Text(stringResource(R.string.help_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.help_intro), style = MaterialTheme.typography.bodySmall)
                }
            }
            items(helpSections.filter { it.first == part }) { (_, title, text) ->
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(text), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
