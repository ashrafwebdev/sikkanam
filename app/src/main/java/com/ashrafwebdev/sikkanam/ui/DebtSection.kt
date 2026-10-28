package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.Debt
import com.ashrafwebdev.sikkanam.data.DebtDirection
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Money lent to people and borrowed from them. Shows what is still owed each way, the due dates,
 * and lets the user record repayments. Nothing is sent anywhere; it stays on the phone.
 */
@Composable
fun DebtSection(vm: FinanceViewModel) {
    val debts by vm.debts.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var repaying by remember { mutableStateOf<Debt?>(null) }

    val lentOwed = debts.filter { it.direction == DebtDirection.LENT }.sumOf { it.remainingCents }
    val borrowedOwed = debts.filter { it.direction == DebtDirection.BORROWED }.sumOf { it.remainingCents }

    SectionCard {
        SectionHeader(
            title = stringResource(R.string.debt_title),
            action = {
                FilledTonalButton(onClick = { showNew = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.debt_add))
                }
            },
        )
        Text(stringResource(R.string.debt_lent_total, formatMoney(lentOwed)), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.debt_borrowed_total, formatMoney(borrowedOwed)), style = MaterialTheme.typography.bodyMedium)

        if (debts.isEmpty()) {
            Text(stringResource(R.string.debt_empty), style = MaterialTheme.typography.bodySmall)
        }
        debts.forEach { debt ->
            DebtRow(debt, onRepay = { repaying = debt }, onDelete = { vm.deleteDebt(debt.id) })
        }
    }

    if (showNew) {
        NewDebtDialog(
            onDismiss = { showNew = false },
            onSave = { person, direction, amount, dueMillis, note ->
                if (vm.saveDebt(person, direction, amount, dueMillis, note)) {
                    showNew = false
                    true
                } else {
                    false
                }
            },
        )
    }

    repaying?.let { debt ->
        RepaymentDialog(
            debt = debt,
            onDismiss = { repaying = null },
            onSave = { amount ->
                if (vm.recordRepayment(debt, amount)) {
                    repaying = null
                    true
                } else {
                    false
                }
            },
        )
    }
}

@Composable
private fun DebtRow(debt: Debt, onRepay: () -> Unit, onDelete: () -> Unit) {
    val overdue = debt.remainingCents > 0 && debt.dueMillis != 0L && debt.dueMillis < startOfDay(System.currentTimeMillis())
    val direction = if (debt.direction == DebtDirection.LENT) stringResource(R.string.debt_lent)
    else stringResource(R.string.debt_borrowed)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${debt.person} · $direction",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.debt_remaining, formatMoney(debt.remainingCents), formatMoney(debt.amountCents)),
                    style = MaterialTheme.typography.bodySmall,
                )
                when {
                    debt.remainingCents == 0L -> Text(stringResource(R.string.debt_settled), style = MaterialTheme.typography.bodySmall)
                    overdue -> Text(
                        stringResource(R.string.debt_overdue, dateLabel(debt.dueMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                    debt.dueMillis != 0L -> Text(
                        stringResource(R.string.debt_due_on, dateLabel(debt.dueMillis)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.debt_delete_desc))
            }
        }
        if (debt.remainingCents > 0) {
            OutlinedButton(onClick = onRepay) { Text(stringResource(R.string.debt_repay)) }
        }
    }
}

/** Date typed as dd/mm/yyyy. Returns 0 for an empty field, the date's millis if valid, or null if invalid. */
private fun parseDueDate(text: String): Long? {
    if (text.isBlank()) return 0L
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }.parse(dateWithSlashes(text).trim())?.time
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun NewDebtDialog(
    onDismiss: () -> Unit,
    onSave: (String, DebtDirection, String, Long, String) -> Boolean,
) {
    var direction by remember { mutableStateOf(DebtDirection.LENT) }
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val dueError = stringResource(R.string.debt_date_error)
    val amountError = stringResource(R.string.debt_amount_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppChip(
                        selected = direction == DebtDirection.LENT,
                        onClick = { direction = DebtDirection.LENT },
                        label = { Text(stringResource(R.string.debt_lent)) },
                    )
                    AppChip(
                        selected = direction == DebtDirection.BORROWED,
                        onClick = { direction = DebtDirection.BORROWED },
                        label = { Text(stringResource(R.string.debt_borrowed)) },
                    )
                }
                OutlinedTextField(value = person, onValueChange = { person = it }, label = { Text(stringResource(R.string.debt_person)) }, singleLine = true)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.debt_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = due,
                    onValueChange = { due = dateDigits(it) },
                    visualTransformation = DigitsVisualTransformation(::dateWithSlashes),
                    label = { Text(stringResource(R.string.debt_due)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.debt_note)) }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dueMillis = parseDueDate(due)
                if (dueMillis == null) {
                    error = dueError
                } else if (!onSave(person, direction, amount, dueMillis, note)) {
                    error = amountError
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun RepaymentDialog(debt: Debt, onDismiss: () -> Unit, onSave: (String) -> Boolean) {
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val repayError = stringResource(R.string.debt_repay_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_repay_title, debt.person)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.debt_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Text(
                    stringResource(R.string.debt_remaining, formatMoney(debt.remainingCents), formatMoney(debt.amountCents)),
                    style = MaterialTheme.typography.bodySmall,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(amount)) error = repayError }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
