package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.GoalEntry
import com.ashrafwebdev.sikkanam.data.SavingsGoal
import java.util.Locale

/** Savings goals with progress bars, and a history of every amount put toward each goal. Shown on the Budgets tab. */
@Composable
fun GoalsSection(vm: FinanceViewModel) {
    val goals by vm.goals.collectAsState()
    val entries by vm.goalEntries.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<SavingsGoal?>(null) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var editingEntry by remember { mutableStateOf<GoalEntry?>(null) }
    // Goals whose history is currently open.
    var openHistory by remember { mutableStateOf(setOf<Long>()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.goal_title), style = MaterialTheme.typography.titleMedium)
        FilledTonalButton(onClick = { showNew = true }) { Text(stringResource(R.string.goal_add)) }

        if (goals.isEmpty()) {
            Text(stringResource(R.string.goal_empty))
        }

        goals.forEach { goal ->
            val fraction = (goal.savedCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)
            val history = entries.filter { it.goalId == goal.id }
            val isOpen = goal.id in openHistory
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editingGoal = goal }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.goal_edit_desc))
                        }
                        IconButton(onClick = { vm.deleteGoal(goal.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.goal_delete_desc))
                        }
                    }
                    Text(stringResource(R.string.goal_saved_of, formatMoney(goal.savedCents), formatMoney(goal.targetCents)))
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(onClick = { adding = goal }) { Text(stringResource(R.string.goal_add_money)) }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            openHistory = if (isOpen) openHistory - goal.id else openHistory + goal.id
                        }) {
                            Text(
                                if (isOpen) stringResource(R.string.goal_hide_history)
                                else stringResource(R.string.goal_history, history.size),
                            )
                        }
                    }

                    if (isOpen) {
                        if (history.isEmpty()) {
                            Text(stringResource(R.string.goal_no_history), style = MaterialTheme.typography.bodySmall)
                        }
                        history.forEach { entry ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        stringResource(R.string.goal_added_sign, formatMoney(entry.amountCents)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val line = listOf(dateLabel(entry.dateMillis), entry.note).filter { it.isNotBlank() }
                                    Text(line.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { editingEntry = entry }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.goal_edit_amount_desc))
                                }
                                IconButton(onClick = { vm.deleteGoalEntry(entry) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.goal_delete_amount_desc))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNew) {
        NewGoalDialog(
            onDismiss = { showNew = false },
            onSave = { name, target ->
                if (vm.saveGoal(name, target)) {
                    showNew = false
                    true
                } else {
                    false
                }
            },
        )
    }

    editingGoal?.let { goal ->
        GoalEditDialog(
            goal = goal,
            onDismiss = { editingGoal = null },
            onSave = { name, target ->
                if (vm.updateGoal(goal, name, target)) {
                    editingGoal = null
                    true
                } else {
                    false
                }
            },
        )
    }

    adding?.let { goal ->
        GoalEntryDialog(
            title = stringResource(R.string.goal_add_money_title, goal.name),
            existing = null,
            onDismiss = { adding = null },
            onSave = { amount, note ->
                if (vm.addToGoal(goal, amount, note)) {
                    adding = null
                    true
                } else {
                    false
                }
            },
        )
    }

    editingEntry?.let { entry ->
        GoalEntryDialog(
            title = stringResource(R.string.goal_correct_title),
            existing = entry,
            onDismiss = { editingEntry = null },
            onSave = { amount, note ->
                if (vm.updateGoalEntry(entry, amount, note)) {
                    editingEntry = null
                    true
                } else {
                    false
                }
            },
        )
    }
}

@Composable
private fun NewGoalDialog(onDismiss: () -> Unit, onSave: (String, String) -> Boolean) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val nameError = stringResource(R.string.goal_name_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.goal_name_hint)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text(stringResource(R.string.goal_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!onSave(name, target)) error = nameError
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun GoalEditDialog(goal: SavingsGoal, onDismiss: () -> Unit, onSave: (String, String) -> Boolean) {
    var name by remember { mutableStateOf(goal.name) }
    var target by remember { mutableStateOf(String.format(Locale.US, "%.2f", goal.targetCents / 100.0)) }
    var error by remember { mutableStateOf<String?>(null) }
    val nameError = stringResource(R.string.goal_name_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.goal_name_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text(stringResource(R.string.goal_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(name, target)) error = nameError }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Used to add an amount to a goal, or to correct an amount already in its history. */
@Composable
private fun GoalEntryDialog(
    title: String,
    existing: GoalEntry?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Boolean,
) {
    var amount by remember {
        mutableStateOf(existing?.let { String.format(Locale.US, "%.2f", it.amountCents / 100.0) } ?: "")
    }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val amountError = stringResource(R.string.goal_amount_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.label_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.goal_note_optional)) },
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!onSave(amount, note)) error = amountError
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
