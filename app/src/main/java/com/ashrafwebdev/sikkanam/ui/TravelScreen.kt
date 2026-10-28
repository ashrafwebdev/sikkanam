package com.ashrafwebdev.sikkanam.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.TravelBooking
import com.ashrafwebdev.sikkanam.data.TravelMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The two parts of the travel page, shown as chips at the top. */
private enum class TravelPart(@StringRes val labelRes: Int) {
    UPCOMING(R.string.section_upcoming),
    PAST(R.string.section_past),
}

/** Bus, train and flight bookings recorded by hand. Upcoming and past trips are listed separately. */
@Composable
fun TravelScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    val bookings by vm.travelBookings.collectAsState()
    val groups by vm.tripGroups.collectAsState()
    val groupNames = groups.associate { it.id to it.name }
    val now = System.currentTimeMillis()
    val upcoming = bookings.filter { it.departMillis >= now }
    val past = bookings.filter { it.departMillis < now }.sortedByDescending { it.departMillis }
    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TravelBooking?>(null) }
    var deleting by remember { mutableStateOf<TravelBooking?>(null) }

    var section by rememberSaveable { mutableStateOf(TravelPart.UPCOMING) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionChips(
            options = TravelPart.entries,
            selected = section,
            label = { stringResource(it.labelRes) },
            onSelect = { section = it },
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SectionCard {
                    SectionHeader(
                        title = stringResource(R.string.travel_title),
                        action = {
                            FilledTonalIconButton(onClick = { showNew = true }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
                            }
                        },
                    )
                    Text(stringResource(R.string.travel_intro), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (bookings.isEmpty()) {
                item { EmptyState(stringResource(R.string.travel_empty)) }
            }
            if (section == TravelPart.UPCOMING && upcoming.isNotEmpty()) {
                items(upcoming, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        groupName = groupNames[booking.groupId],
                        onEdit = { editing = booking },
                        onDelete = { deleting = booking },
                    )
                }
            }
            if (section == TravelPart.PAST && past.isNotEmpty()) {
                items(past, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        groupName = groupNames[booking.groupId],
                        onEdit = { editing = booking },
                        onDelete = { deleting = booking },
                    )
                }
            }
        }
    }

    if (showNew) {
        TravelBookingDialog(vm, existing = null, initialGroupId = 0L, onDismiss = { showNew = false })
    }
    editing?.let { booking ->
        TravelBookingDialog(vm, existing = booking, initialGroupId = 0L, onDismiss = { editing = null })
    }
    deleting?.let { booking ->
        TravelDeleteDialog(
            onConfirm = {
                vm.deleteTravel(booking.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

/** The travel of one jamaat trip: its linked bookings, and a button to book one for this trip. */
@Composable
fun TripTravelSection(vm: FinanceViewModel, groupId: Long) {
    val bookings by vm.travelBookings.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TravelBooking?>(null) }
    var deleting by remember { mutableStateOf<TravelBooking?>(null) }
    val linked = bookings.filter { it.groupId == groupId }

    SectionCard {
        SectionHeader(
            title = stringResource(R.string.travel_trip_title),
            action = {
                FilledTonalIconButton(onClick = { showNew = true }) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
    }
            },
        )
        if (linked.isEmpty()) {
            Text(stringResource(R.string.travel_trip_empty), style = MaterialTheme.typography.bodySmall)
        }
        linked.forEach { booking ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(booking.mode.labelRes) + ": " +
                        stringResource(R.string.travel_route, booking.fromPlace, booking.toPlace) +
                        ", " + dateTimeLabel(booking.departMillis),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { editing = booking }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.travel_edit_desc))
                }
                IconButton(onClick = { deleting = booking }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.travel_delete_desc))
                }
            }
        }
    }

    if (showNew) {
        TravelBookingDialog(vm, existing = null, initialGroupId = groupId, onDismiss = { showNew = false })
    }
    editing?.let { booking ->
        TravelBookingDialog(vm, existing = booking, initialGroupId = groupId, onDismiss = { editing = null })
    }
    deleting?.let { booking ->
        TravelDeleteDialog(
            onConfirm = {
                vm.deleteTravel(booking.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun TravelDeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(R.string.travel_delete_confirm)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun BookingCard(booking: TravelBooking, groupName: String?, onEdit: () -> Unit, onDelete: () -> Unit) {
    val modeLabel = stringResource(booking.mode.labelRes)
    val title = if (booking.operator.isBlank()) modeLabel else "$modeLabel: ${booking.operator}"
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.travel_route, booking.fromPlace, booking.toPlace),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(R.string.travel_departs, dateTimeLabel(booking.departMillis)),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (booking.arriveMillis != 0L) {
                    Text(
                        stringResource(R.string.travel_arrives, dateTimeLabel(booking.arriveMillis)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (booking.reference.isNotBlank()) {
                    Text(stringResource(R.string.travel_reference, booking.reference), style = MaterialTheme.typography.bodySmall)
                }
                if (booking.seat.isNotBlank()) {
                    Text(stringResource(R.string.travel_seat, booking.seat), style = MaterialTheme.typography.bodySmall)
                }
                if (booking.fareCents != 0L) {
                    Text(stringResource(R.string.travel_fare, formatMoney(booking.fareCents)), style = MaterialTheme.typography.bodySmall)
                }
                if (groupName != null) {
                    Text(stringResource(R.string.travel_trip, groupName), style = MaterialTheme.typography.bodySmall)
                }
                if (booking.notes.isNotBlank()) {
                    Text(booking.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.travel_edit_desc))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.travel_delete_desc))
            }
        }
    }
}

/** Add or edit form. [initialGroupId] pre-selects a jamaat trip when the form is opened from that trip. */
@Composable
fun TravelBookingDialog(vm: FinanceViewModel, existing: TravelBooking?, initialGroupId: Long, onDismiss: () -> Unit) {
    val groups by vm.tripGroups.collectAsState()
    var mode by remember { mutableStateOf(existing?.mode ?: TravelMode.BUS) }
    var operator by remember { mutableStateOf(existing?.operator.orEmpty()) }
    var fromPlace by remember { mutableStateOf(existing?.fromPlace.orEmpty()) }
    var toPlace by remember { mutableStateOf(existing?.toPlace.orEmpty()) }
    var departDate by remember { mutableStateOf(dateLabel(existing?.departMillis ?: System.currentTimeMillis())) }
    var departTime by remember { mutableStateOf(existing?.let { timeLabel(it.departMillis) }.orEmpty()) }
    var arriveDate by remember {
        mutableStateOf(existing?.takeIf { it.arriveMillis != 0L }?.let { dateLabel(it.arriveMillis) }.orEmpty())
    }
    var arriveTime by remember {
        mutableStateOf(existing?.takeIf { it.arriveMillis != 0L }?.let { timeLabel(it.arriveMillis) }.orEmpty())
    }
    var reference by remember { mutableStateOf(existing?.reference.orEmpty()) }
    var fare by remember {
        mutableStateOf(existing?.takeIf { it.fareCents != 0L }?.let { formatPlain(it.fareCents) }.orEmpty())
    }
    var seat by remember { mutableStateOf(existing?.seat.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var groupId by remember { mutableStateOf(existing?.groupId ?: initialGroupId) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.travel_add_title else R.string.travel_edit_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.travel_field_mode), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TravelMode.entries.forEach { option ->
                        AppChip(
                            selected = mode == option,
                            onClick = { mode = option },
                            label = { Text(stringResource(option.labelRes)) },
                        )
                    }
                }
                TravelField(operator, { operator = it }, R.string.travel_field_operator)
                TravelField(fromPlace, { fromPlace = it }, R.string.travel_field_from)
                TravelField(toPlace, { toPlace = it }, R.string.travel_field_to)
                TravelField(departDate, { departDate = dateDigits(it) }, R.string.travel_field_depart_date, KeyboardType.Number, DigitsVisualTransformation(::dateWithSlashes))
                TravelField(departTime, { departTime = timeDigits(it) }, R.string.travel_field_depart_time, KeyboardType.Number, DigitsVisualTransformation(::timeWithColon))
                TravelField(arriveDate, { arriveDate = dateDigits(it) }, R.string.travel_field_arrive_date, KeyboardType.Number, DigitsVisualTransformation(::dateWithSlashes))
                TravelField(arriveTime, { arriveTime = timeDigits(it) }, R.string.travel_field_arrive_time, KeyboardType.Number, DigitsVisualTransformation(::timeWithColon))
                TravelField(reference, { reference = it }, R.string.travel_field_reference)
                TravelField(fare, { fare = it }, R.string.travel_field_fare, KeyboardType.Decimal)
                TravelField(seat, { seat = it }, R.string.travel_field_seat)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.travel_field_notes)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.travel_field_group), style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppChip(
                        selected = groupId == 0L,
                        onClick = { groupId = 0L },
                        label = { Text(stringResource(R.string.travel_group_none)) },
                    )
                    groups.forEach { group ->
                        AppChip(
                            selected = groupId == group.id,
                            onClick = { groupId = group.id },
                            label = { Text(group.name) },
                        )
                    }
                }
                if (showError) {
                    Text(
                        stringResource(R.string.travel_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val depart = parseDateTime(departDate, departTime)
                val arrive = if (arriveDate.isBlank() && arriveTime.isBlank()) 0L else parseDateTime(arriveDate, arriveTime)
                val saved = depart != null && arrive != null && vm.saveTravel(
                    existing = existing,
                    mode = mode,
                    operator = operator,
                    fromPlace = fromPlace,
                    toPlace = toPlace,
                    departMillis = depart,
                    arriveMillis = arrive,
                    reference = reference,
                    fareText = fare,
                    seat = seat,
                    notes = notes,
                    groupId = groupId,
                )
                if (saved) {
                    onDismiss()
                } else {
                    showError = true
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun TravelField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        visualTransformation = visualTransformation,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun timeLabel(millis: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))

private fun dateTimeLabel(millis: Long): String = dateLabel(millis) + " " + timeLabel(millis)

/** Reads a dd/mm/yyyy date and an hh:mm time typed by the user. Null when either is not valid. */
private fun parseDateTime(date: String, time: String): Long? = runCatching {
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply { isLenient = false }
        .parse("${dateWithSlashes(date).trim()} ${timeWithColon(time).trim()}")?.time
}.getOrNull()
