package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.DutyAssignment

/**
 * Duties for today's mashwara in one jamaat. The people are the jamaat's members. Each day the jamaat
 * decides who takes each duty, and ticks it when it's done. The duty list is shared by all jamaats and
 * can be changed: add a duty, rename it, or remove it.
 */
@Composable
fun DutiesSection(vm: FinanceViewModel, groupId: Long, members: List<String>) {
    val allAssignments by vm.dutyAssignments.collectAsState()
    val assignments = allAssignments.filter { it.groupId == groupId }
    val dutyTypes by vm.dutyTypes.collectAsState()
    val names = dutyTypes.map { it.name }
    var newDuty by remember { mutableStateOf("") }
    var dutyError by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<String?>(null) }
    val dutyNameError = stringResource(R.string.duty_name_error)

    val today = startOfDay(System.currentTimeMillis())
    val todays = assignments.filter { it.dayMillis == today }

    SectionCard {
        SectionHeader(title = stringResource(R.string.duty_title))

        Text(stringResource(R.string.duty_list_title), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newDuty,
                onValueChange = {
                    newDuty = it
                    dutyError = null
                },
                label = { Text(stringResource(R.string.duty_new)) },
                singleLine = true,
                isError = dutyError != null,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = {
                if (vm.addDutyType(newDuty)) {
                    newDuty = ""
                } else {
                    dutyError = dutyNameError
                }
            }) { Text(stringResource(R.string.action_add)) }
        }
        dutyError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        if (members.isEmpty()) {
            Text(stringResource(R.string.duty_add_people_first), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(stringResource(R.string.duty_today), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.duty_decide), style = MaterialTheme.typography.bodySmall)
            names.forEach { duty ->
                val assignment: DutyAssignment? = todays.firstOrNull { it.duty == duty }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        CategorySelector(
                            label = duty,
                            options = members,
                            selected = assignment?.personName.orEmpty(),
                            onSelect = { vm.assignDuty(today, duty, it, groupId) },
                        )
                    }
                    if (assignment != null) {
                        Checkbox(checked = assignment.done, onCheckedChange = { vm.setDutyDone(assignment, it) })
                    }
                    IconButton(onClick = { renaming = duty }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.duty_rename_desc))
                    }
                    IconButton(onClick = { vm.deleteDutyType(duty) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.duty_remove_desc))
                    }
                }
            }

            Text(stringResource(R.string.duty_so_far), style = MaterialTheme.typography.labelLarge)
            // Every duty each member has been given in this jamaat, across all days recorded.
            members.forEach { person ->
                val received = assignments.filter { it.personName == person }.map { it.duty }.toSet()
                val remaining = names.filter { it !in received }
                val line = stringResource(R.string.duty_count, person, names.count { it in received }, names.size) +
                    if (remaining.isEmpty()) "" else stringResource(R.string.duty_not_yet, remaining.joinToString(", "))
                Text(
                    line,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
    }

    renaming?.let { oldName ->
        var newName by remember(oldName) { mutableStateOf(oldName) }
        var error by remember(oldName) { mutableStateOf<String?>(null) }
        val renameError = stringResource(R.string.duty_rename_error)
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.duty_rename_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.duty_name_label)) },
                        singleLine = true,
                    )
                    Text(
                        stringResource(R.string.duty_rename_info),
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
                        error = renameError
                    }
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
