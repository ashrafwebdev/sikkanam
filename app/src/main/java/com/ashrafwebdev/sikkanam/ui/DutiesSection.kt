package com.ashrafwebdev.sikkanam.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.DutyAssignment

private const val PREFS = "sikkanam_prefs"
private const val KEY_PEOPLE = "mashwara_people"

/** The people in the mashwara group, kept on the phone. */
fun loadMashwaraPeople(context: Context): List<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_PEOPLE, "")
        .orEmpty()
        .split('\n')
        .filter { it.isNotBlank() }

fun saveMashwaraPeople(context: Context, people: List<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PEOPLE, people.joinToString("\n"))
        .apply()
}

/**
 * Duties for today's mashwara. Each day the group decides who takes each duty, and ticks it when
 * it's done. The duty list can be changed: add a duty, rename it, or remove it.
 */
@Composable
fun DutiesSection(vm: FinanceViewModel) {
    val context = LocalContext.current
    val assignments by vm.dutyAssignments.collectAsState()
    val dutyTypes by vm.dutyTypes.collectAsState()
    val names = dutyTypes.map { it.name }
    var people by remember { mutableStateOf(loadMashwaraPeople(context)) }
    var newPerson by remember { mutableStateOf("") }
    var newDuty by remember { mutableStateOf("") }
    var dutyError by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<String?>(null) }

    val today = startOfDay(System.currentTimeMillis())
    val todays = assignments.filter { it.dayMillis == today }

    SectionCard {
        SectionHeader(title = "Duties (khidmat)")

        Text("People in the group", style = MaterialTheme.typography.labelLarge)
        people.forEach { name ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    people = people - name
                    saveMashwaraPeople(context, people)
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove person")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newPerson,
                onValueChange = { newPerson = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = {
                val clean = newPerson.trim()
                if (clean.isNotBlank() && people.none { it.equals(clean, ignoreCase = true) }) {
                    people = people + clean
                    saveMashwaraPeople(context, people)
                    newPerson = ""
                }
            }) { Text("Add") }
        }

        Text("Duties", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newDuty,
                onValueChange = {
                    newDuty = it
                    dutyError = null
                },
                label = { Text("New duty") },
                singleLine = true,
                isError = dutyError != null,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = {
                if (vm.addDutyType(newDuty)) {
                    newDuty = ""
                } else {
                    dutyError = "Enter a new name up to 60 characters."
                }
            }) { Text("Add") }
        }
        dutyError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (people.isEmpty()) {
            Text("Add the people in the group first.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Today's duties", style = MaterialTheme.typography.labelLarge)
            Text("Decide in the mashwara who takes each duty, then tick it when done.", style = MaterialTheme.typography.bodySmall)
            names.forEach { duty ->
                val assignment: DutyAssignment? = todays.firstOrNull { it.duty == duty }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        CategorySelector(
                            label = duty,
                            options = people,
                            selected = assignment?.personName.orEmpty(),
                            onSelect = { vm.assignDuty(today, duty, it) },
                        )
                    }
                    if (assignment != null) {
                        Checkbox(checked = assignment.done, onCheckedChange = { vm.setDutyDone(assignment, it) })
                    }
                    IconButton(onClick = { renaming = duty }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename duty")
                    }
                    IconButton(onClick = { vm.deleteDutyType(duty) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove duty")
                    }
                }
            }

            Text("So far", style = MaterialTheme.typography.labelLarge)
            // Every duty each person has been given, across all days recorded.
            people.forEach { person ->
                val received = assignments.filter { it.personName == person }.map { it.duty }.toSet()
                val remaining = names.filter { it !in received }
                Text(
                    "$person: ${names.count { it in received }} of ${names.size} duties" +
                        if (remaining.isEmpty()) "" else ". Not yet: ${remaining.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }

    renaming?.let { oldName ->
        var newName by remember(oldName) { mutableStateOf(oldName) }
        var error by remember(oldName) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename duty") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Duty name") },
                        singleLine = true,
                    )
                    Text(
                        "Past records of this duty are renamed too.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (vm.renameDutyType(oldName, newName)) {
                        renaming = null
                    } else {
                        error = "Enter a unique name up to 60 characters."
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }
}
