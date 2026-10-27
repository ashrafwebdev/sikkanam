package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.Budget
import com.ashrafwebdev.sikkanam.data.TransactionType

@Composable
fun BudgetsScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    val budgets by vm.budgets.collectAsState()
    val monthTx by vm.monthTransactions.collectAsState()
    val categories by vm.categories.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Budget?>(null) }

    // Money spent per category this month, used to show each budget's progress.
    val spentByCategory = monthTx
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amountCents } }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            GoalsSection(vm)
        }

        item {
            SectionHeader(
                title = "Monthly budgets",
                action = {
                    FilledTonalButton(onClick = { editing = null; showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                },
            )
        }

        if (budgets.isEmpty()) {
            item {
                EmptyState("No budgets yet. Set a monthly limit for a category to see how you're doing.")
            }
        }

        items(budgets, key = { it.category }) { budget ->
            val spent = spentByCategory[budget.category] ?: 0L
            val ratio = spent.toFloat() / budget.monthlyLimitCents
            val over = spent > budget.monthlyLimitCents
            val barColor = when {
                over -> MaterialTheme.colorScheme.error
                ratio >= 0.8f -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            }
            val left = budget.monthlyLimitCents - spent

            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        budget.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { editing = budget; showDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Change limit")
                    }
                    IconButton(onClick = { vm.deleteBudget(budget.category) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete budget")
                    }
                }
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Spent ${formatMoney(spent)} of ${formatMoney(budget.monthlyLimitCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (over) "Over by ${formatMoney(-left)}" else "Left ${formatMoney(left)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (over) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                }
            }
        }
    }

    if (showDialog) {
        BudgetDialog(
            existing = editing,
            categories = categories,
            onDismiss = { showDialog = false },
            onSave = { category, limit ->
                if (vm.saveBudget(category, limit)) showDialog = false
            },
        )
    }
}

@Composable
private fun BudgetDialog(
    existing: Budget?,
    categories: List<com.ashrafwebdev.sikkanam.data.Category>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var limit by remember {
        mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.monthlyLimitCents / 100.0) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add budget" else "Change budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategorySelector(
                    label = "Category",
                    type = TransactionType.EXPENSE,
                    options = categories.filter { it.type == TransactionType.EXPENSE }.map { it.name },
                    selected = category,
                    onSelect = { category = it },
                    enabled = existing == null,
                )
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Monthly limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(category, limit) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
