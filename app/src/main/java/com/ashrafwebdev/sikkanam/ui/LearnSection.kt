package com.ashrafwebdev.sikkanam.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.learn.LearnCategory
import com.ashrafwebdev.sikkanam.learn.LearnContent
import com.ashrafwebdev.sikkanam.learn.LearnItem

private const val PREFS = "sikkanam_prefs"
private const val KEY_LEARNED = "learn_done"

/** The ids of the items marked as learned, kept on the phone. */
fun loadLearned(context: Context): Set<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY_LEARNED, emptySet()).orEmpty()

fun saveLearned(context: Context, ids: Set<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(KEY_LEARNED, ids).apply()
}

/**
 * Things to learn for the trip: surahs, duas, adhan, prayers, and halal and haram. Each item shows
 * the Arabic, the transliterations, and the meanings in Tamil and English. Items can be ticked once learned.
 */
@Composable
fun LearnSection() {
    val context = LocalContext.current
    var learned by remember { mutableStateOf(loadLearned(context)) }

    val surahs = LearnContent.categories.first()
    val surahsLearned = surahs.items.count { it.id in learned }

    SectionCard {
        SectionHeader(title = "Learn for the trip")
        Text(
            "Surahs learned: $surahsLearned of ${surahs.items.size}. " +
                "Check the content with a teacher before teaching it.",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    LearnContent.categories.forEach { category ->
        LearnCategoryCard(category, learned) { id, checked ->
            learned = if (checked) learned + id else learned - id
            saveLearned(context, learned)
        }
    }
}

@Composable
private fun LearnCategoryCard(category: LearnCategory, learned: Set<String>, onToggle: (String, Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val done = category.items.count { it.id in learned }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f).clickable { open = !open }) {
                Text(category.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("$done of ${category.items.size} learned", style = MaterialTheme.typography.bodySmall)
            }
            Icon(
                if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (open) "Collapse" else "Expand",
                modifier = Modifier.clickable { open = !open },
            )
        }
        if (open) {
            category.items.forEach { item ->
                LearnItemRow(item, item.id in learned) { onToggle(item.id, it) }
            }
        }
    }
}

@Composable
private fun LearnItemRow(item: LearnItem, isLearned: Boolean, onLearned: (Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).clickable { open = !open },
            )
            Checkbox(checked = isLearned, onCheckedChange = onLearned)
        }
        if (open) {
            if (item.arabic.isNotBlank()) {
                Text(
                    item.arabic,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Tamil: ${item.tamilTransliteration}", style = MaterialTheme.typography.bodyMedium)
                Text("English: ${item.englishTransliteration}", style = MaterialTheme.typography.bodyMedium)
            }
            if (item.meaningTamil.isNotBlank()) {
                Text("Meaning (Tamil): ${item.meaningTamil}", style = MaterialTheme.typography.bodySmall)
            }
            if (item.meaningEnglish.isNotBlank()) {
                Text("Meaning (English): ${item.meaningEnglish}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
