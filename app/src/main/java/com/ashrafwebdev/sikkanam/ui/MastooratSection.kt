package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
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
import com.ashrafwebdev.sikkanam.data.MahramRelation
import com.ashrafwebdev.sikkanam.data.MastooratEffort
import com.ashrafwebdev.sikkanam.data.MastooratRules
import com.ashrafwebdev.sikkanam.data.MastooratTrip
import com.ashrafwebdev.sikkanam.data.PairInput
import com.ashrafwebdev.sikkanam.data.PairProblem
import com.ashrafwebdev.sikkanam.data.Person
import com.ashrafwebdev.sikkanam.data.PersonChoice
import com.ashrafwebdev.sikkanam.data.TaleemKind
import com.ashrafwebdev.sikkanam.data.TashkeelKind
import java.text.SimpleDateFormat
import java.util.Locale

/** A pair row as typed in the form. Only the mahram man is named. A row with no mahram and no age is left out. */
private data class PairRow(
    val mahram: PersonChoice = PersonChoice(),
    val childAge: String = "",
    val relation: MahramRelation = MahramRelation.OTHER,
)

private fun relationRes(relation: MahramRelation): Int = when (relation) {
    MahramRelation.WIFE -> R.string.mastoorat_rel_wife
    MahramRelation.MOTHER -> R.string.mastoorat_rel_mother
    MahramRelation.DAUGHTER -> R.string.mastoorat_rel_daughter
    MahramRelation.SISTER -> R.string.mastoorat_rel_sister
    MahramRelation.OTHER -> R.string.mastoorat_rel_other
}

private fun effortRes(effort: MastooratEffort): Int = when (effort) {
    MastooratEffort.DAYS_3 -> R.string.mastoorat_effort_3
    MastooratEffort.DAYS_10 -> R.string.mastoorat_effort_10
    MastooratEffort.DAYS_40 -> R.string.mastoorat_effort_40
    MastooratEffort.MONTHS_2_FOREIGN -> R.string.mastoorat_effort_2m
}

private fun taleemRes(kind: TaleemKind): Int = when (kind) {
    TaleemKind.WEEKLY_IJTIMA -> R.string.mastoorat_taleem_weekly
    TaleemKind.DAILY_HOME -> R.string.mastoorat_taleem_daily
}

private fun tashkeelRes(kind: TashkeelKind): Int = when (kind) {
    TashkeelKind.THREE_TIMES_THREE_DAYS -> R.string.mastoorat_tashkeel_3x3
    TashkeelKind.TEN_DAYS -> R.string.mastoorat_tashkeel_10
    TashkeelKind.FORTY_DAYS -> R.string.mastoorat_tashkeel_40
    TashkeelKind.TWO_MONTHS -> R.string.mastoorat_tashkeel_2m
}

/** Reads a dd/mm/yyyy date typed by the user, at the start of that day. Null when it is not a valid date. */
private fun parseDay(text: String): Long? = runCatching {
    SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }.parse(text.trim())?.time
}.getOrNull()

/** The limit messages, read once while the form is composed so the click handlers can use them. */
private class PairMessages(val tooMany: String, val childPairs: String, val childAge: String, val childNotAllowed: String) {
    /** The problem with these pairs in this effort, as a message, or null when they are allowed. */
    fun problemOf(effort: MastooratEffort, pairs: List<PairInput>): String? = when (MastooratRules.check(effort, pairs)) {
        null -> null
        PairProblem.TOO_MANY_PAIRS -> tooMany
        PairProblem.TOO_MANY_CHILD_PAIRS -> childPairs
        PairProblem.CHILD_TOO_YOUNG -> childAge
        PairProblem.CHILD_NOT_ALLOWED -> childNotAllowed
    }
}

@Composable
private fun rememberPairMessages(): PairMessages = PairMessages(
    tooMany = stringResource(R.string.mastoorat_err_pairs, MastooratRules.MAX_PAIRS),
    childPairs = stringResource(R.string.mastoorat_err_child_pairs, MastooratRules.MAX_CHILD_PAIRS),
    childAge = stringResource(R.string.mastoorat_err_child_age, MastooratRules.MIN_CHILD_AGE),
    childNotAllowed = stringResource(R.string.mastoorat_err_child_not_allowed),
)

/**
 * The Mastoorat section of a Mastoorat group: the women's efforts (3, 10 and 40 days, and 2 months foreign),
 * the pairs with their mahram men, the weekly ijtima and daily taleem with who has which part, and tashkeel.
 * Only men's names are recorded, from the people list. The women's names are never written down.
 */
@Composable
fun MastooratSection(vm: FinanceViewModel, groupId: Long) {
    val trips by vm.mastooratTrips.collectAsState()
    val pairs by vm.mastooratPairs.collectAsState()
    val taleem by vm.mastooratTaleem.collectAsState()
    val tashkeel by vm.mastooratTashkeel.collectAsState()
    val register by vm.people.collectAsState()
    val names = register.associate { it.id to it.name }
    val myTrips = trips.filter { it.groupId == groupId }
    val myTaleem = taleem.filter { it.groupId == groupId }
    val myTashkeel = tashkeel.filter { it.groupId == groupId }

    var showNewTrip by remember { mutableStateOf(false) }
    var addingPairsTo by remember { mutableStateOf<MastooratTrip?>(null) }
    var deletingTrip by remember { mutableStateOf<MastooratTrip?>(null) }
    var showTaleem by remember { mutableStateOf(false) }
    var showTashkeel by remember { mutableStateOf(false) }

    SectionCard {
        SectionHeader(title = stringResource(R.string.mastoorat_title))
        Text(stringResource(R.string.mastoorat_intro), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { showNewTrip = true }) { Text(stringResource(R.string.mastoorat_new_trip)) }
            OutlinedButton(onClick = { showTaleem = true }) { Text(stringResource(R.string.mastoorat_record_taleem)) }
            OutlinedButton(onClick = { showTashkeel = true }) { Text(stringResource(R.string.mastoorat_record_tashkeel)) }
        }
    }

    SectionCard {
        SectionHeader(title = stringResource(R.string.mastoorat_efforts_title))
        MastooratEffort.entries.forEach { effort ->
            val effortTrips = myTrips.filter { it.effort == effort }
            val tripIds = effortTrips.map { it.id }.toSet()
            val effortPairs = pairs.filter { it.tripId in tripIds }
            val withChild = effortPairs.count { it.childAge != 0 }
            Text(
                stringResource(
                    R.string.mastoorat_effort_summary,
                    stringResource(effortRes(effort)),
                    effortTrips.size,
                    effortPairs.size,
                    withChild,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    SectionCard {
        SectionHeader(title = stringResource(R.string.mastoorat_trips_title))
        if (myTrips.isEmpty()) {
            Text(stringResource(R.string.mastoorat_trips_empty), style = MaterialTheme.typography.bodySmall)
        }
        myTrips.forEach { trip ->
            val tripPairs = pairs.filter { it.tripId == trip.id }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(effortRes(trip.effort)) + " · " + dateLabel(trip.startMillis) + " – " + dateLabel(trip.endMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { deletingTrip = trip }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.mastoorat_delete_desc))
                    }
                }
                if (trip.place.isNotBlank()) {
                    Text(stringResource(R.string.mastoorat_place, trip.place), style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(R.string.mastoorat_pairs_count, tripPairs.size, MastooratRules.MAX_PAIRS),
                    style = MaterialTheme.typography.bodySmall,
                )
                tripPairs.forEach { pair ->
                    val child = if (pair.childAge != 0) stringResource(R.string.mastoorat_child_line, pair.childAge) else ""
                    Text(
                        stringResource(
                            R.string.mastoorat_pair_line,
                            names[pair.mahramPersonId].orEmpty(),
                            stringResource(relationRes(pair.relation)),
                        ) + child,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (MastooratRules.canAddPairsLater(trip.effort) && tripPairs.size < MastooratRules.MAX_PAIRS) {
                    TextButton(onClick = { addingPairsTo = trip }) { Text(stringResource(R.string.mastoorat_add_pairs)) }
                } else if (!MastooratRules.canAddPairsLater(trip.effort)) {
                    Text(stringResource(R.string.mastoorat_no_extra_pairs), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    SectionCard {
        SectionHeader(title = stringResource(R.string.mastoorat_taleem_title))
        val weekly = myTaleem.filter { it.kind == TaleemKind.WEEKLY_IJTIMA }
        val onTarget = weekly.count { MastooratRules.taleemOnTarget(it.attended) }
        Text(
            stringResource(R.string.mastoorat_taleem_summary, weekly.size, onTarget, myTaleem.count { it.kind == TaleemKind.DAILY_HOME }),
            style = MaterialTheme.typography.bodySmall,
        )
        // Who has done each part, so the duties are shared fairly and easy to see.
        if (myTaleem.isNotEmpty()) {
            Text(stringResource(R.string.mastoorat_duties_title), style = MaterialTheme.typography.labelLarge)
            register.forEach { person ->
                val reads = myTaleem.count { it.readerPersonId == person.id }
                val reception = myTaleem.count { it.receptionPersonId == person.id }
                val sixPoints = myTaleem.count { it.sixPointsPersonId == person.id }
                val tashkeelDuty = myTaleem.count { it.tashkeelPersonId == person.id }
                if (reads + reception + sixPoints + tashkeelDuty > 0) {
                    Text(
                        stringResource(R.string.mastoorat_duty_line, person.name, reads, reception, sixPoints, tashkeelDuty),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        myTaleem.take(20).forEach { entry ->
            val offTarget = entry.kind == TaleemKind.WEEKLY_IJTIMA && !MastooratRules.taleemOnTarget(entry.attended)
            RecordRow(
                title = stringResource(taleemRes(entry.kind)) + " · " + dateLabel(entry.dayMillis),
                detail = stringResource(R.string.mastoorat_attended_line, entry.house, entry.attended) +
                    if (offTarget) " · " + stringResource(R.string.mastoorat_off_target) else "",
                roles = listOf(
                    stringResource(R.string.mastoorat_role_reader) to names[entry.readerPersonId].orEmpty(),
                    stringResource(R.string.mastoorat_role_reception) to names[entry.receptionPersonId].orEmpty(),
                    stringResource(R.string.mastoorat_role_six_points) to names[entry.sixPointsPersonId].orEmpty(),
                    stringResource(R.string.mastoorat_role_tashkeel) to names[entry.tashkeelPersonId].orEmpty(),
                ),
                onDelete = { vm.deleteMastooratTaleem(entry.id) },
            )
        }
    }

    SectionCard {
        SectionHeader(title = stringResource(R.string.mastoorat_tashkeel_title))
        TashkeelKind.entries.forEach { kind ->
            Text(
                stringResource(tashkeelRes(kind)) + ": " + myTashkeel.filter { it.kind == kind }.sumOf { it.joined },
                style = MaterialTheme.typography.bodySmall,
            )
        }
        myTashkeel.take(20).forEach { entry ->
            RecordRow(
                title = stringResource(tashkeelRes(entry.kind)) + " · " + dateLabel(entry.dayMillis),
                detail = stringResource(R.string.mastoorat_joined_line, entry.joined),
                roles = emptyList(),
                onDelete = { vm.deleteMastooratTashkeel(entry.id) },
            )
        }
    }

    if (showNewTrip) {
        NewMastooratTripDialog(
            people = register,
            onDismiss = { showNewTrip = false },
            onSave = { effort, start, end, place, inputs -> vm.saveMastooratTrip(groupId, effort, start, end, place, inputs) },
            onSaved = { showNewTrip = false },
        )
    }
    addingPairsTo?.let { trip ->
        AddPairsDialog(
            trip = trip,
            people = register,
            existing = pairs.filter { it.tripId == trip.id }
                .map { PairInput(PersonChoice(personId = it.mahramPersonId), it.childAge, it.relation) },
            onDismiss = { addingPairsTo = null },
            onSave = { inputs -> vm.addMastooratPairs(trip, inputs) },
            onSaved = { addingPairsTo = null },
        )
    }
    deletingTrip?.let { trip ->
        AlertDialog(
            onDismissRequest = { deletingTrip = null },
            text = { Text(stringResource(R.string.mastoorat_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteMastooratTrip(trip)
                    deletingTrip = null
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { deletingTrip = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (showTaleem) {
        RecordTaleemDialog(
            people = register,
            onDismiss = { showTaleem = false },
            onSave = { kind, day, house, attended, reader, reception, sixPoints, tashkeelPerson ->
                vm.saveMastooratTaleem(groupId, kind, day, house, attended, reader, reception, sixPoints, tashkeelPerson)
            },
            onSaved = { showTaleem = false },
        )
    }
    if (showTashkeel) {
        RecordTashkeelDialog(
            onDismiss = { showTashkeel = false },
            onSave = { kind, day, joined -> vm.saveMastooratTashkeel(groupId, kind, day, joined) },
            onSaved = { showTashkeel = false },
        )
    }
}

@Composable
private fun RecordRow(title: String, detail: String, roles: List<Pair<String, String>>, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            roles.forEach { (label, name) ->
                Text(
                    label + ": " + name.ifBlank { stringResource(R.string.mastoorat_role_none) },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.mastoorat_delete_desc))
        }
    }
}

/** Starts a trip: its effort, dates, place, and the mahram men of the pairs going out. */
@Composable
private fun NewMastooratTripDialog(
    people: List<Person>,
    onDismiss: () -> Unit,
    onSave: (MastooratEffort, Long, Long, String, List<PairInput>) -> Boolean,
    onSaved: () -> Unit,
) {
    var effort by remember { mutableStateOf(MastooratEffort.DAYS_3) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    val rows = remember { mutableStateListOf(PairRow()) }
    var error by remember { mutableStateOf<String?>(null) }
    val dateError = stringResource(R.string.mastoorat_date_error)
    val pairError = stringResource(R.string.mastoorat_pairs_error)
    val messages = rememberPairMessages()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mastoorat_new_trip)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EffortChips(effort) { effort = it }
                DateField(start, { start = it }, R.string.mastoorat_start_date)
                DateField(end, { end = it }, R.string.mastoorat_end_date)
                OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text(stringResource(R.string.mastoorat_place_field)) }, singleLine = true)
                PairRows(rows, people)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startMillis = parseDay(start)
                val endMillis = parseDay(end)
                val inputs = pairInputs(rows)
                error = when {
                    startMillis == null || endMillis == null || endMillis < startMillis -> dateError
                    inputs.isEmpty() -> pairError
                    else -> messages.problemOf(effort, inputs)
                }
                if (error == null && onSave(effort, startMillis!!, endMillis!!, place, inputs)) onSaved()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Adds pairs to a short trip, up to the limit, keeping the pairs already on it. */
@Composable
private fun AddPairsDialog(
    trip: MastooratTrip,
    people: List<Person>,
    existing: List<PairInput>,
    onDismiss: () -> Unit,
    onSave: (List<PairInput>) -> Boolean,
    onSaved: () -> Unit,
) {
    val rows = remember { mutableStateListOf(PairRow()) }
    var error by remember { mutableStateOf<String?>(null) }
    val pairError = stringResource(R.string.mastoorat_pairs_error)
    val messages = rememberPairMessages()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mastoorat_add_pairs)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PairRows(rows, people)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val inputs = pairInputs(rows)
                error = when {
                    inputs.isEmpty() -> pairError
                    else -> messages.problemOf(trip.effort, existing + inputs) ?: if (onSave(inputs)) null else pairError
                }
                if (error == null) onSaved()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** One taleem meeting: the type, the day, the house, the women who came, and who has each part. */
@Composable
private fun RecordTaleemDialog(
    people: List<Person>,
    onDismiss: () -> Unit,
    onSave: (TaleemKind, Long, String, String, PersonChoice, PersonChoice, PersonChoice, PersonChoice) -> Boolean,
    onSaved: () -> Unit,
) {
    var kind by remember { mutableStateOf(TaleemKind.WEEKLY_IJTIMA) }
    var day by remember { mutableStateOf("") }
    var house by remember { mutableStateOf("") }
    var attended by remember { mutableStateOf("") }
    var reader by remember { mutableStateOf(PersonChoice()) }
    var reception by remember { mutableStateOf(PersonChoice()) }
    var sixPoints by remember { mutableStateOf(PersonChoice()) }
    var tashkeelPerson by remember { mutableStateOf(PersonChoice()) }
    var error by remember { mutableStateOf<String?>(null) }
    val dateError = stringResource(R.string.mastoorat_date_error)
    val numberError = stringResource(R.string.mastoorat_number_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mastoorat_record_taleem)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaleemKind.entries.forEach { option ->
                        FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(stringResource(taleemRes(option))) })
                    }
                }
                DateField(day, { day = it }, R.string.mastoorat_day)
                OutlinedTextField(value = house, onValueChange = { house = it }, label = { Text(stringResource(R.string.mastoorat_house)) }, singleLine = true)
                OutlinedTextField(
                    value = attended,
                    onValueChange = { attended = it },
                    label = { Text(stringResource(R.string.mastoorat_women_count)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Text(stringResource(R.string.mastoorat_roles_title), style = MaterialTheme.typography.labelLarge)
                PersonPickerFields(
                    people = people,
                    choice = reader,
                    onChoice = { reader = it },
                    label = stringResource(R.string.mastoorat_role_reader),
                    showPhone = false,
                )
                PersonPickerFields(
                    people = people,
                    choice = reception,
                    onChoice = { reception = it },
                    label = stringResource(R.string.mastoorat_role_reception),
                    showPhone = false,
                )
                PersonPickerFields(
                    people = people,
                    choice = sixPoints,
                    onChoice = { sixPoints = it },
                    label = stringResource(R.string.mastoorat_role_six_points),
                    showPhone = false,
                )
                PersonPickerFields(
                    people = people,
                    choice = tashkeelPerson,
                    onChoice = { tashkeelPerson = it },
                    label = stringResource(R.string.mastoorat_role_tashkeel),
                    showPhone = false,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dayMillis = parseDay(day)
                error = when {
                    dayMillis == null -> dateError
                    attended.trim().toIntOrNull() == null -> numberError
                    else -> null
                }
                if (error == null && onSave(kind, dayMillis!!, house, attended, reader, reception, sixPoints, tashkeelPerson)) onSaved()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun RecordTashkeelDialog(
    onDismiss: () -> Unit,
    onSave: (TashkeelKind, Long, String) -> Boolean,
    onSaved: () -> Unit,
) {
    var kind by remember { mutableStateOf(TashkeelKind.THREE_TIMES_THREE_DAYS) }
    var day by remember { mutableStateOf("") }
    var joined by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val dateError = stringResource(R.string.mastoorat_date_error)
    val numberError = stringResource(R.string.mastoorat_number_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mastoorat_record_tashkeel)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TashkeelKind.entries.forEach { option ->
                        FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(stringResource(tashkeelRes(option))) })
                    }
                }
                DateField(day, { day = it }, R.string.mastoorat_day)
                OutlinedTextField(
                    value = joined,
                    onValueChange = { joined = it },
                    label = { Text(stringResource(R.string.mastoorat_joined_count)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val dayMillis = parseDay(day)
                error = when {
                    dayMillis == null -> dateError
                    joined.trim().toIntOrNull() == null -> numberError
                    else -> null
                }
                if (error == null && onSave(kind, dayMillis!!, joined)) onSaved()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun EffortChips(selected: MastooratEffort, onSelect: (MastooratEffort) -> Unit) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MastooratEffort.entries.forEach { option ->
            FilterChip(selected = selected == option, onClick = { onSelect(option) }, label = { Text(stringResource(effortRes(option))) })
        }
    }
}

@Composable
private fun DateField(value: String, onChange: (String) -> Unit, labelRes: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        placeholder = { Text(stringResource(R.string.mastoorat_date_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The pair rows of a form. Each row is a mahram man from the people list, and the age of a female child (blank for none). */
@Composable
private fun PairRows(rows: MutableList<PairRow>, people: List<Person>) {
    Text(stringResource(R.string.mastoorat_pairs_title), style = MaterialTheme.typography.labelLarge)
    rows.forEachIndexed { index, row ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PersonPickerFields(
                people = people,
                choice = row.mahram,
                onChoice = { rows[index] = row.copy(mahram = it) },
                label = stringResource(R.string.mastoorat_mahram),
                showPhone = false,
            )
            Text(stringResource(R.string.mastoorat_relation_label), style = MaterialTheme.typography.labelSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MahramRelation.entries.forEach { option ->
                    FilterChip(
                        selected = row.relation == option,
                        onClick = { rows[index] = row.copy(relation = option) },
                        label = { Text(stringResource(relationRes(option))) },
                    )
                }
            }
            OutlinedTextField(
                value = row.childAge,
                onValueChange = { rows[index] = row.copy(childAge = it) },
                label = { Text(stringResource(R.string.mastoorat_child_age)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (rows.size < MastooratRules.MAX_PAIRS) {
        OutlinedButton(onClick = { rows.add(PairRow()) }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.mastoorat_add_pair_row))
        }
    }
}

/** The pairs typed in the rows. Rows with no mahram and no age are skipped. */
private fun pairInputs(rows: List<PairRow>): List<PairInput> = rows
    .filter { it.mahram.name.isNotBlank() || it.mahram.personId != 0L || it.childAge.isNotBlank() }
    .map { PairInput(it.mahram, it.childAge.trim().toIntOrNull() ?: 0, it.relation) }
