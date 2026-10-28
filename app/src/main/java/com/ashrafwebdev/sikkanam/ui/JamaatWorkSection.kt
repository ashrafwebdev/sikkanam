package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.Amaal
import com.ashrafwebdev.sikkanam.data.Khurooj
import com.ashrafwebdev.sikkanam.data.KhuroojType
import com.ashrafwebdev.sikkanam.data.TripMember
import com.ashrafwebdev.sikkanam.data.WeeklyProgramme
import java.text.SimpleDateFormat
import java.util.Locale

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

private fun amaalLabelRes(amaal: Amaal): Int = when (amaal) {
    Amaal.MASJID_WARD_FIKR -> R.string.amaal_masjid_ward_fikr
    Amaal.TAALEEM_MASJID -> R.string.amaal_taaleem_masjid
    Amaal.TAALEEM_HOUSE -> R.string.amaal_taaleem_house
    Amaal.MUQAMI_GASHT -> R.string.amaal_muqami_gasht
    Amaal.SECOND_GASHT -> R.string.amaal_second_gasht
    Amaal.MASJID_ABAD -> R.string.amaal_masjid_abad
    Amaal.THREE_DAYS_JAMAAT -> R.string.amaal_three_days
}

fun khuroojLabelRes(type: KhuroojType): Int = when (type) {
    KhuroojType.MEN_40_DAYS_INDIA -> R.string.khurooj_men_40_india
    KhuroojType.MEN_4_MONTHS_INDIA -> R.string.khurooj_men_4m_india
    KhuroojType.MEN_5_MONTHS_FOREIGN -> R.string.khurooj_men_5m_foreign
    KhuroojType.WOMEN_3_DAYS_INDIA -> R.string.khurooj_women_3_days
    KhuroojType.WOMEN_10_DAYS_INDIA -> R.string.khurooj_women_10_days
    KhuroojType.WOMEN_40_DAYS_INDIA -> R.string.khurooj_women_40_india
    KhuroojType.WOMEN_2_MONTHS_FOREIGN -> R.string.khurooj_women_2m_foreign
    KhuroojType.WOMEN_2_MONTHS_NIZAMUDDIN -> R.string.khurooj_women_2m_nizamuddin
    KhuroojType.OTHER -> R.string.khurooj_other
}

/**
 * Muqami amaal for one jamaat: each member has a tick for each amaal for today, and a count for the last
 * seven days (seven amaal a day, so 49 is the most).
 */
@Composable
fun MuqamiAmaalSection(vm: FinanceViewModel, groupId: Long, members: List<TripMember>) {
    val allChecks by vm.amaalChecks.collectAsState()
    val checks = allChecks.filter { it.groupId == groupId }
    val today = startOfDay(System.currentTimeMillis())
    val weekStart = today - 6 * DAY_MILLIS
    val all = Amaal.entries

    SectionCard {
        SectionHeader(title = stringResource(R.string.amaal_title))
        Text(stringResource(R.string.amaal_desc), style = MaterialTheme.typography.bodySmall)
        if (members.isEmpty()) {
            Text(stringResource(R.string.amaal_add_members), style = MaterialTheme.typography.bodySmall)
        }
        members.forEach { member ->
            val mine = checks.filter { it.memberId == member.id }
            val weekDone = mine.count { it.done && it.dayMillis >= weekStart && it.dayMillis <= today }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.amaal_week_count, weekDone, 7 * all.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    all.forEach { amaal ->
                        val done = mine.any { it.amaal == amaal.key && it.dayMillis == today && it.done }
                        AppChip(
                            selected = done,
                            onClick = { vm.setAmaal(groupId, member.id, today, amaal, !done) },
                            label = { Text(stringResource(amaalLabelRes(amaal))) },
                        )
                    }
                }
            }
        }
    }
}

/** The first day (Sunday, local midnight) of the week containing [millis]. */
private fun startOfWeek(millis: Long): Long {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = startOfDay(millis) }
    cal.add(java.util.Calendar.DAY_OF_MONTH, -(cal.get(java.util.Calendar.DAY_OF_WEEK) - java.util.Calendar.SUNDAY))
    return cal.timeInMillis
}

private fun weeklyLabelRes(programme: WeeklyProgramme): Int = when (programme) {
    WeeklyProgramme.HALQA_MASHWARA -> R.string.weekly_halqa
    WeeklyProgramme.CITY_MASHWARA -> R.string.weekly_city
    WeeklyProgramme.SHABGUJARI -> R.string.weekly_shabgujari
}

/** Weekly halqa mashwara, city mashwara and shabgujari: tick who attended this week, for each member. */
@Composable
fun WeeklyProgrammeSection(vm: FinanceViewModel, groupId: Long, members: List<TripMember>) {
    val allChecks by vm.amaalChecks.collectAsState()
    val checks = allChecks.filter { it.groupId == groupId }
    val weekStart = startOfWeek(System.currentTimeMillis())
    val programmes = WeeklyProgramme.entries

    SectionCard {
        SectionHeader(title = stringResource(R.string.weekly_title))
        Text(stringResource(R.string.weekly_desc), style = MaterialTheme.typography.bodySmall)
        if (members.isEmpty()) {
            Text(stringResource(R.string.amaal_add_members), style = MaterialTheme.typography.bodySmall)
        }
        members.forEach { member ->
            val mine = checks.filter { it.memberId == member.id && it.dayMillis == weekStart && it.done }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(member.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.weekly_count, mine.size, programmes.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    programmes.forEach { programme ->
                        val done = mine.any { it.amaal == programme.key }
                        AppChip(
                            selected = done,
                            onClick = { vm.setWeekly(groupId, member.id, weekStart, programme, !done) },
                            label = { Text(stringResource(weeklyLabelRes(programme))) },
                        )
                    }
                }
            }
        }
    }
}

/** Khurooj records for one jamaat: type, dates, places and a note. */
@Composable
fun KhuroojSection(vm: FinanceViewModel, groupId: Long) {
    val all by vm.khurooj.collectAsState()
    val records = all.filter { it.groupId == groupId }
    var showNew by remember { mutableStateOf(false) }

    SectionCard {
        SectionHeader(
            title = stringResource(R.string.khurooj_title),
            action = {
                FilledTonalButton(onClick = { showNew = true }) { Text(stringResource(R.string.action_add)) }
            },
        )
        if (records.isEmpty()) {
            Text(stringResource(R.string.khurooj_empty), style = MaterialTheme.typography.bodySmall)
        }
        records.forEach { record ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(khuroojLabelRes(record.type)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val dates = if (record.endMillis == 0L) stringResource(R.string.khurooj_from, dateLabel(record.startMillis))
                    else stringResource(R.string.khurooj_dates, dateLabel(record.startMillis), dateLabel(record.endMillis))
                    Text(listOf(dates, record.places).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    if (record.note.isNotBlank()) Text(record.note, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { vm.deleteKhurooj(record.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.khurooj_delete_desc))
                }
            }
        }
    }

    if (showNew) {
        NewKhuroojDialog(
            onDismiss = { showNew = false },
            onSave = { type, start, end, places, note ->
                if (vm.saveKhurooj(groupId, type, start, end, places, note)) {
                    showNew = false
                    true
                } else {
                    false
                }
            },
        )
    }
}

/** dd/mm/yyyy to millis. Returns 0 for an empty field, or null if the text is not a valid date. */
private fun parseDay(text: String): Long? {
    if (text.isBlank()) return 0L
    return try {
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }.parse(text.trim())?.time
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun NewKhuroojDialog(
    onDismiss: () -> Unit,
    onSave: (KhuroojType, Long, Long, String, String) -> Boolean,
) {
    var type by remember { mutableStateOf(KhuroojType.MEN_40_DAYS_INDIA) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var places by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val dateError = stringResource(R.string.khurooj_date_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.khurooj_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // One per line, so each long label fits on a phone screen.
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KhuroojType.entries.forEach { option ->
                        AppChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(stringResource(khuroojLabelRes(option))) },
                        )
                    }
                }
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = formatDateTyping(it) },
                    label = { Text(stringResource(R.string.khurooj_start)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = formatDateTyping(it) },
                    label = { Text(stringResource(R.string.khurooj_end)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(value = places, onValueChange = { places = it }, label = { Text(stringResource(R.string.khurooj_places)) }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.khurooj_note)) }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startMillis = parseDay(start)
                val endMillis = parseDay(end)
                if (startMillis == null || endMillis == null || startMillis == 0L) {
                    error = dateError
                } else {
                    onSave(type, startMillis, endMillis, places, note)
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
