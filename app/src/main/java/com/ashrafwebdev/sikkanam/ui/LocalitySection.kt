package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.KhuroojType
import com.ashrafwebdev.sikkanam.data.LocalityPerson
import com.ashrafwebdev.sikkanam.data.MeetingVisit
import com.ashrafwebdev.sikkanam.data.Person

/** Splits a comma-separated list of khurooj type names back into types. Unknown names are ignored. */
private fun parseTypes(text: String): Set<KhuroojType> =
    text.split(",").mapNotNull { name -> KhuroojType.entries.firstOrNull { it.name == name.trim() } }.toSet()

/**
 * The register of people living in the masjid's locality: their khurooj history, which khurooj they are
 * available for, meetings (with the taskeel given at each), and a monthly list of who was given taskeel
 * for each khurooj type.
 */
@Composable
fun LocalitySection(vm: FinanceViewModel) {
    val people by vm.localityPeople.collectAsState()
    val register by vm.people.collectAsState()
    val visits by vm.meetingVisits.collectAsState()
    val today = startOfDay(System.currentTimeMillis())
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LocalityPerson?>(null) }
    var meetingWith by remember { mutableStateOf<LocalityPerson?>(null) }
    var monthStart by remember { mutableStateOf(startOfMonth(System.currentTimeMillis())) }

    val total = people.size
    val wentAny = people.count { it.wentFortyDays || it.wentFourMonths }
    val forty = people.count { it.wentFortyDays }
    val fourMonths = people.count { it.wentFourMonths }
    val active = people.count { it.active }

    SectionCard {
        SectionHeader(
            title = stringResource(R.string.locality_title),
            action = {
                FilledTonalButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_add))
                }
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.locality_total, total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.locality_went, wentAny), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.locality_forty, forty), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.locality_four_months, fourMonths), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.locality_active, active, total - active), style = MaterialTheme.typography.bodySmall)
        }
        if (people.isEmpty()) {
            Text(stringResource(R.string.locality_empty), style = MaterialTheme.typography.bodySmall)
        }
        people.forEach { person ->
            val mine = visits.filter { it.personId == person.id }
            val lastDay = mine.maxOfOrNull { it.dayMillis }
            val metToday = mine.any { it.dayMillis == today }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        person.name + if (person.locality.isNotBlank()) " · ${person.locality}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val history = listOfNotNull(
                        if (person.wentFortyDays) stringResource(R.string.locality_tag_forty) else null,
                        if (person.wentFourMonths) stringResource(R.string.locality_tag_four_months) else null,
                    )
                    val status = stringResource(if (person.active) R.string.locality_status_active else R.string.locality_status_inactive)
                    Text((listOf(status) + history).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    val available = parseTypes(person.availableTypes)
                    if (available.isNotEmpty()) {
                        // Look the labels up first: a composable call is not allowed inside joinToString's lambda.
                        val availableNames = available.map { stringResource(khuroojLabelRes(it)) }
                        Text(
                            stringResource(
                                R.string.locality_available,
                                availableNames.joinToString(", "),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        if (lastDay == null) stringResource(R.string.locality_never_met)
                        else stringResource(R.string.locality_met_count, mine.size, dateLabel(lastDay)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                FilledTonalButton(enabled = !metToday, onClick = { meetingWith = person }) {
                    Text(stringResource(if (metToday) R.string.locality_met_today_done else R.string.locality_met_today))
                }
                TextButton(onClick = { editing = person }) { Text(stringResource(R.string.action_edit)) }
                IconButton(onClick = { vm.deleteLocalityPerson(person.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.locality_delete_desc))
                }
            }
        }

        // Monthly list: who was given taskeel for each khurooj type in the chosen month.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthStart = addMonths(monthStart, -1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.home_previous_month))
            }
            Text(
                stringResource(R.string.taskeel_month_title, monthLabel(monthStart)),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { monthStart = addMonths(monthStart, 1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.home_next_month))
            }
        }
        val monthEnd = addMonths(monthStart, 1)
        val taskeelThisMonth = visits.filter { it.dayMillis >= monthStart && it.dayMillis < monthEnd && it.taskeel.isNotBlank() }
        if (taskeelThisMonth.isEmpty()) {
            Text(stringResource(R.string.taskeel_month_empty), style = MaterialTheme.typography.bodySmall)
        }
        KhuroojType.entries.forEach { type ->
            val names = taskeelThisMonth
                .filter { it.taskeel == type.name }
                .map { it.personId }
                .distinct()
                .mapNotNull { id -> people.firstOrNull { it.id == id }?.name }
            if (names.isNotEmpty()) {
                Text(
                    stringResource(R.string.taskeel_month_line, stringResource(khuroojLabelRes(type)), names.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(names.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showAdd) {
        LocalityPersonDialog(
            existing = null,
            people = register,
            onDismiss = { showAdd = false },
            onSave = { person, choice ->
                if (vm.addLocalityPerson(person, choice)) {
                    showAdd = false
                    true
                } else {
                    false
                }
            },
        )
    }

    editing?.let { person ->
        LocalityPersonDialog(
            existing = person,
            people = register,
            onDismiss = { editing = null },
            onSave = { updated, _ ->
                if (updated.name.isBlank()) {
                    false
                } else {
                    vm.updateLocalityPerson(updated)
                    editing = null
                    true
                }
            },
        )
    }

    meetingWith?.let { person ->
        TaskeelDialog(
            person = person,
            onDismiss = { meetingWith = null },
            onSave = { taskeel, note ->
                if (vm.recordVisit(person.id, today, taskeel, note)) {
                    meetingWith = null
                    true
                } else {
                    false
                }
            },
        )
    }
}

/** Records a meeting today and the taskeel given: one khurooj type, or none. */
@Composable
private fun TaskeelDialog(person: LocalityPerson, onDismiss: () -> Unit, onSave: (KhuroojType?, String) -> Boolean) {
    var chosen by remember { mutableStateOf<KhuroojType?>(null) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.taskeel_dialog_title, person.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = chosen == null,
                    onClick = { chosen = null },
                    label = { Text(stringResource(R.string.taskeel_none)) },
                )
                KhuroojType.entries.forEach { type ->
                    FilterChip(
                        selected = chosen == type,
                        onClick = { chosen = type },
                        label = { Text(stringResource(khuroojLabelRes(type))) },
                    )
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.goal_note_optional)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(chosen, note) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun LocalityPersonDialog(
    existing: LocalityPerson?,
    people: List<Person>,
    onDismiss: () -> Unit,
    onSave: (LocalityPerson, PersonChoice) -> Boolean,
) {
    // A new person is picked from the register (or added to it). Editing keeps the name and phone as they are.
    var choice by remember {
        mutableStateOf(PersonChoice(personId = existing?.personId ?: 0, name = existing?.name.orEmpty(), phone = existing?.phone.orEmpty()))
    }
    var locality by remember { mutableStateOf(existing?.locality ?: "") }
    var forty by remember { mutableStateOf(existing?.wentFortyDays ?: false) }
    var fourMonths by remember { mutableStateOf(existing?.wentFourMonths ?: false) }
    var active by remember { mutableStateOf(existing?.active ?: false) }
    var available by remember { mutableStateOf(parseTypes(existing?.availableTypes.orEmpty())) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val nameError = stringResource(R.string.dawat_name_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) stringResource(R.string.locality_add_title) else stringResource(R.string.locality_edit_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing == null) {
                    PersonPickerFields(people = people, choice = choice, onChoice = { choice = it })
                } else {
                    OutlinedTextField(
                        value = choice.name,
                        onValueChange = { choice = choice.copy(name = it) },
                        label = { Text(stringResource(R.string.dawat_name)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = choice.phone,
                        onValueChange = { choice = choice.copy(phone = it) },
                        label = { Text(stringResource(R.string.dawat_phone)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                    )
                }
                OutlinedTextField(value = locality, onValueChange = { locality = it }, label = { Text(stringResource(R.string.locality_field)) }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = forty, onClick = { forty = !forty }, label = { Text(stringResource(R.string.locality_tag_forty)) })
                    FilterChip(selected = fourMonths, onClick = { fourMonths = !fourMonths }, label = { Text(stringResource(R.string.locality_tag_four_months)) })
                }
                FilterChip(
                    selected = active,
                    onClick = { active = !active },
                    label = { Text(stringResource(R.string.locality_status_active)) },
                )
                Text(stringResource(R.string.locality_available_label), style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KhuroojType.entries.forEach { type ->
                        FilterChip(
                            selected = type in available,
                            onClick = { available = if (type in available) available - type else available + type },
                            label = { Text(stringResource(khuroojLabelRes(type))) },
                        )
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.dawat_notes)) }, minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val person = LocalityPerson(
                    id = existing?.id ?: 0,
                    name = choice.name,
                    phone = choice.phone,
                    locality = locality,
                    wentFortyDays = forty,
                    wentFourMonths = fourMonths,
                    active = active,
                    notes = notes,
                    availableTypes = available.joinToString(",") { it.name },
                    personId = existing?.personId ?: 0,
                )
                if (!onSave(person, choice)) error = nameError
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
