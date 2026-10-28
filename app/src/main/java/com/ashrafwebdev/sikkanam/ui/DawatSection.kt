package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
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
import com.ashrafwebdev.sikkanam.data.DawatContact
import com.ashrafwebdev.sikkanam.data.DawatStatus
import com.ashrafwebdev.sikkanam.data.Person
import com.ashrafwebdev.sikkanam.data.PersonChoice
import java.text.SimpleDateFormat
import java.util.Locale

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

private fun statusRes(status: DawatStatus): Int = when (status) {
    DawatStatus.MET -> R.string.dawat_status_met
    DawatStatus.INTERESTED -> R.string.dawat_status_interested
    DawatStatus.TASKEEL_MUQAMI -> R.string.dawat_status_muqami
    DawatStatus.TASKEEL_KHUROOJ -> R.string.dawat_status_khurooj
    DawatStatus.AGREED_KHUROOJ -> R.string.dawat_status_agreed
    DawatStatus.NOT_NOW -> R.string.dawat_status_not_now
}

/**
 * Dawat and taskeel: the people met each day, what they agreed to (muqami amaal or khurooj), follow-up
 * dates, and forming a new jamaat from those who agreed to khurooj. Shown on the Jamaat list.
 */
@Composable
fun DawatSection(vm: FinanceViewModel) {
    val contacts by vm.dawatContacts.collectAsState()
    val people by vm.people.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var followUpFor by remember { mutableStateOf<DawatContact?>(null) }
    var showForm by remember { mutableStateOf(false) }

    val today = startOfDay(System.currentTimeMillis())
    val dueToday = contacts.count { it.followUpMillis != 0L && it.followUpMillis < today + DAY_MILLIS }
    val agreed = contacts.count { it.status == DawatStatus.AGREED_KHUROOJ }

    SectionCard {
        SectionHeader(
            title = stringResource(R.string.dawat_title),
            action = {
                FilledTonalIconButton(onClick = { showAdd = true }) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
    }
            },
        )
        Text(
            stringResource(R.string.dawat_summary, contacts.size, dueToday, agreed),
            style = MaterialTheme.typography.bodySmall,
        )
        if (agreed > 0) {
            OutlinedButton(onClick = { showForm = true }) { Text(stringResource(R.string.dawat_form_jamaat)) }
        }
        if (contacts.isEmpty()) {
            Text(stringResource(R.string.dawat_empty), style = MaterialTheme.typography.bodySmall)
        }
        contacts.forEach { person ->
            val due = person.followUpMillis != 0L && person.followUpMillis < today + DAY_MILLIS
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        person.name + if (person.area.isNotBlank()) " · ${person.area}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    // Tap the status to move it to the next step.
                    Text(
                        stringResource(statusRes(person.status)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { vm.setDawatStatus(person, person.status.next()) },
                    )
                    if (person.followUpMillis != 0L) {
                        Text(
                            stringResource(
                                if (due) R.string.dawat_followup_due else R.string.dawat_followup,
                                dateLabel(person.followUpMillis),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (due) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = { followUpFor = person }) { Text(stringResource(R.string.dawat_set_followup)) }
                IconButton(onClick = { vm.deleteDawatContact(person.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.dawat_delete_desc))
                }
            }
        }
    }

    if (showAdd) {
        AddDawatDialog(
            people = people,
            onDismiss = { showAdd = false },
            onSave = { choice, area, notes ->
                if (vm.addDawatContact(choice, area, notes)) {
                    showAdd = false
                    true
                } else {
                    false
                }
            },
        )
    }

    followUpFor?.let { person ->
        FollowUpDialog(
            person = person,
            onDismiss = { followUpFor = null },
            onSave = { millis ->
                vm.setDawatFollowUp(person, millis)
                followUpFor = null
                true
            },
        )
    }

    if (showForm) {
        FormJamaatDialog(
            agreed = contacts.filter { it.status == DawatStatus.AGREED_KHUROOJ },
            onDismiss = { showForm = false },
            onCreate = { name, selected ->
                if (vm.formJamaat(name, selected)) {
                    showForm = false
                    true
                } else {
                    false
                }
            },
        )
    }
}

/** dd/mm/yyyy to millis. Returns 0 for an empty field, or null if the text is not a valid date. */
private fun parseDate(text: String): Long? {
    if (text.isBlank()) return 0L
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }.parse(dateWithSlashes(text).trim())?.time
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun AddDawatDialog(
    people: List<Person>,
    onDismiss: () -> Unit,
    onSave: (PersonChoice, String, String) -> Boolean,
) {
    var choice by remember { mutableStateOf(PersonChoice()) }
    var area by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val nameError = stringResource(R.string.dawat_name_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dawat_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonPickerFields(people = people, choice = choice, onChoice = { choice = it })
                OutlinedTextField(value = area, onValueChange = { area = it }, label = { Text(stringResource(R.string.dawat_area)) }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.dawat_notes)) }, minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(choice, area, notes)) error = nameError }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun FollowUpDialog(person: DawatContact, onDismiss: () -> Unit, onSave: (Long) -> Boolean) {
    var date by remember { mutableStateOf(if (person.followUpMillis == 0L) "" else dateLabel(person.followUpMillis)) }
    var error by remember { mutableStateOf<String?>(null) }
    val dateError = stringResource(R.string.dawat_date_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dawat_followup_title, person.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = dateDigits(it) },
                    visualTransformation = DigitsVisualTransformation(::dateWithSlashes),
                    label = { Text(stringResource(R.string.dawat_followup_date)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val millis = parseDate(date)
                if (millis == null) error = dateError else onSave(millis)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Forms a new jamaat from the people who agreed to khurooj. Everyone is ticked by default. */
@Composable
private fun FormJamaatDialog(
    agreed: List<DawatContact>,
    onDismiss: () -> Unit,
    onCreate: (String, List<DawatContact>) -> Boolean,
) {
    var name by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf(agreed.map { it.id }.toSet()) }
    var error by remember { mutableStateOf<String?>(null) }
    val formError = stringResource(R.string.dawat_form_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dawat_form_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.dawat_form_name)) }, singleLine = true)
                agreed.forEach { person ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = person.id in chosen,
                            onCheckedChange = { on -> chosen = if (on) chosen + person.id else chosen - person.id },
                        )
                        Text(person.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!onCreate(name, agreed.filter { it.id in chosen })) error = formError
            }) { Text(stringResource(R.string.dawat_form_create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
