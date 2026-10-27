package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.Category
import com.ashrafwebdev.sikkanam.data.Transaction
import com.ashrafwebdev.sikkanam.data.TransactionType

/**
 * Category names offered for [type]: the master list first, then any older category
 * already used on a transaction, so existing entries can still be edited.
 */
fun categoryOptions(type: TransactionType, master: List<Category>, all: List<Transaction>): List<String> {
    val fromMaster = master.filter { it.type == type }.map { it.name }
    val used = all.filter { it.type == type }.map { it.category }
    return (fromMaster + used).distinct()
}

/** How money was paid or received. Fixed, so reports group the same way every time. */
val paymentMethodOptions = listOf("Cash", "Bank transfer", "Debit card", "Credit card", "Mobile wallet", "Cheque", "Other")

/**
 * A field that opens a searchable list of [options]. When [type] is given, the options are
 * grouped by theme under headings. Otherwise they're shown as one flat list.
 */
@Composable
fun CategorySelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
) {
    var open by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { open = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (selected.isBlank()) "Choose one" else selected)
            }
        }
    }

    if (open) {
        CategoryPickerDialog(
            title = label,
            options = options,
            type = type,
            onDismiss = { open = false },
            onPick = {
                onSelect(it)
                open = false
            },
        )
    }
}

@Composable
private fun CategoryPickerDialog(
    title: String,
    options: List<String>,
    type: TransactionType?,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matching = options.filter { it.contains(query.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                    if (type == null) {
                        items(matching) { option ->
                            OptionRow(option) { onPick(option) }
                        }
                    } else {
                        groupCategories(matching, type).forEach { (group, names) ->
                            item {
                                Text(
                                    group,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                                )
                            }
                            items(names) { option ->
                                OptionRow(option) { onPick(option) }
                            }
                        }
                    }
                    if (matching.isEmpty()) {
                        item { Text("No match. Add it in Settings first.") }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun OptionRow(option: String, onClick: () -> Unit) {
    Text(
        option,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    )
}
