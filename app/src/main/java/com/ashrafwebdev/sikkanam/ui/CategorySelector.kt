package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ashrafwebdev.sikkanam.data.Transaction
import com.ashrafwebdev.sikkanam.data.TransactionType

/** Standard categories. Users pick from these, so each category has one spelling. */
object Categories {
    val expense = listOf("Food", "Transport", "Bills", "Rent", "Shopping", "Health", "Education", "Family", "Charity", "Other")
    val income = listOf("Salary", "Business", "Gifts", "Other")
}

/** Standard categories for [type], plus any older category already used with that type. */
fun categoryOptions(type: TransactionType, all: List<Transaction>): List<String> {
    val standard = if (type == TransactionType.INCOME) Categories.income else Categories.expense
    val used = all.filter { it.type == type }.map { it.category }
    return (standard + used).distinct()
}

/** A dropdown that only offers [options], so the chosen value always matches an existing category. */
@Composable
fun CategorySelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (selected.isBlank()) "Choose a category" else selected)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
