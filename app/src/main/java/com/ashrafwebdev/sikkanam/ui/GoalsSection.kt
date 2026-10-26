package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.SavingsGoal

/** Savings goals with progress bars. Shown at the top of the Budgets tab. */
@Composable
fun GoalsSection(vm: FinanceViewModel) {
    val goals by vm.goals.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf<SavingsGoal?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Savings goals", style = MaterialTheme.typography.titleMedium)
        FilledTonalButton(onClick = { showNew = true }) { Text("Add goal") }

        if (goals.isEmpty()) {
            Text("No goals yet. Add a target to track money you're setting aside.")
        }

        goals.forEach { goal ->
            val fraction = (goal.savedCents.toFloat() / goal.targetCents).coerceIn(0f, 1f)
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(goal.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.deleteGoal(goal.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete goal")
                        }
                    }
                    Text("${formatMoney(goal.savedCents)} of ${formatMoney(goal.targetCents)}")
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { adding = goal }) { Text("Add money") }
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

    adding?.let { goal ->
        AddMoneyDialog(
            goal = goal,
            onDismiss = { adding = null },
            onSave = { amount ->
                if (vm.addToGoal(goal, amount)) {
                    adding = null
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New savings goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal, for example House deposit") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!onSave(name, target)) error = "Enter a name and a target above zero."
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddMoneyDialog(goal: SavingsGoal, onDismiss: () -> Unit, onSave: (String) -> Boolean) {
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add money to ${goal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!onSave(amount)) error = "Enter an amount above zero."
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
