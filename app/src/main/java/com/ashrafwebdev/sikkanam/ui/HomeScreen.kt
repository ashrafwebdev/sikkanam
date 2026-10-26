package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.HalalStatus
import com.ashrafwebdev.sikkanam.data.Transaction
import com.ashrafwebdev.sikkanam.data.TransactionType

@Composable
fun HomeScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                writer.write(transactionsToCsv(vm.transactions.value))
            }
        }
    }

    val monthStart by vm.monthStart.collectAsState()
    val monthTx by vm.monthTransactions.collectAsState()
    val visible by vm.visibleTransactions.collectAsState()
    val query by vm.query.collectAsState()
    val typeFilter by vm.typeFilter.collectAsState()

    var editing by remember { mutableStateOf<Transaction?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val income = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
    val expense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
    val interest = monthTx.filter { it.isRiba }.sumOf { it.amountCents }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Hijri date: ${hijriToday()}", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.shiftMonth(-1) }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    monthLabel(monthStart),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { vm.shiftMonth(1) }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Balance", style = MaterialTheme.typography.labelLarge)
                    Text(formatMoney(income - expense), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Income: ${formatMoney(income)}")
                        Text("Expenses: ${formatMoney(expense)}")
                    }
                    if (interest > 0) {
                        Text("Interest paid (riba): ${formatMoney(interest)}")
                    }
                }
            }
        }

        item {
            CategoryBreakdown(monthTx)
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { editing = null; showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
                TextButton(onClick = { exportLauncher.launch("sikkanam-transactions.csv") }) { Text("Export CSV") }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = { Text("Search category or note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = typeFilter == null, onClick = { vm.setTypeFilter(null) }, label = { Text("All") })
                FilterChip(
                    selected = typeFilter == TransactionType.INCOME,
                    onClick = { vm.setTypeFilter(TransactionType.INCOME) },
                    label = { Text("Income") },
                )
                FilterChip(
                    selected = typeFilter == TransactionType.EXPENSE,
                    onClick = { vm.setTypeFilter(TransactionType.EXPENSE) },
                    label = { Text("Expense") },
                )
            }
        }

        items(visible, key = { it.id }) { transaction ->
            TransactionRow(
                transaction = transaction,
                onClick = { editing = transaction; showDialog = true },
                onDelete = { vm.deleteTransaction(transaction.id) },
            )
        }
    }

    if (showDialog) {
        TransactionDialog(
            existing = editing,
            onDismiss = { showDialog = false },
            onSave = { type, amount, category, note, halal, riba ->
                if (vm.saveTransaction(editing, type, amount, category, note, halal, riba)) showDialog = false
            },
        )
    }
}

@Composable
private fun CategoryBreakdown(monthTx: List<Transaction>) {
    val byCategory = monthTx
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amountCents } }
        .entries
        .sortedByDescending { it.value }
    if (byCategory.isEmpty()) return

    val total = byCategory.sumOf { it.value }.toFloat()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Spending by category", style = MaterialTheme.typography.titleSmall)
            byCategory.forEach { (category, cents) ->
                val fraction = cents / total
                Text("$category: ${formatMoney(cents)}")
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, onClick: () -> Unit, onDelete: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val sign = if (isIncome) "+" else "-"

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val details = buildList {
                    add(dateLabel(transaction.dateMillis))
                    if (transaction.note.isNotBlank()) add(transaction.note)
                    if (isIncome && transaction.halalStatus != HalalStatus.NONE) add(transaction.halalStatus.label())
                    if (transaction.isRiba) add("Interest (riba)")
                }
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            Text("$sign${formatMoney(transaction.amountCents)}", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

private fun HalalStatus.label(): String = when (this) {
    HalalStatus.NONE -> ""
    HalalStatus.HALAL -> "Halal"
    HalalStatus.DOUBTFUL -> "Doubtful"
    HalalStatus.HARAM -> "Haram"
}

@Composable
private fun TransactionDialog(
    existing: Transaction?,
    onDismiss: () -> Unit,
    onSave: (TransactionType, String, String, String, HalalStatus, Boolean) -> Unit,
) {
    var type by remember { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf(existing?.let { formatPlain(it.amountCents) } ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var halal by remember { mutableStateOf(existing?.halalStatus ?: HalalStatus.NONE) }
    var riba by remember { mutableStateOf(existing?.isRiba ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add transaction" else "Edit transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Expense") },
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Income") },
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                )
                if (type == TransactionType.INCOME) {
                    Text("Source (Islamic status)", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(HalalStatus.NONE, HalalStatus.HALAL, HalalStatus.DOUBTFUL, HalalStatus.HARAM).forEach { status ->
                            FilterChip(
                                selected = halal == status,
                                onClick = { halal = status },
                                label = { Text(if (status == HalalStatus.NONE) "Not set" else status.label()) },
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = riba, onCheckedChange = { riba = it })
                        Text("This is interest (riba)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(type, amount, category, note, halal, riba) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
