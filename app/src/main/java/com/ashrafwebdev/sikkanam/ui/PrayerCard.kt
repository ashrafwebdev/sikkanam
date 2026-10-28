package com.ashrafwebdev.sikkanam.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.input.KeyboardType
import com.ashrafwebdev.sikkanam.prayer.MasjidTimings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.prayer.AsrMethod
import com.ashrafwebdev.sikkanam.prayer.City
import com.ashrafwebdev.sikkanam.prayer.PrayerSettings
import com.ashrafwebdev.sikkanam.prayer.PrayerTimes
import com.ashrafwebdev.sikkanam.prayer.calculatePrayerTimes
import com.ashrafwebdev.sikkanam.prayer.cities
import com.ashrafwebdev.sikkanam.prayer.formatPrayerTime
import java.util.Calendar

/** Today's prayer times for the chosen city, shown on the Home screen. Works offline. */
@Composable
fun PrayerCard() {
    val context = LocalContext.current
    val city = PrayerSettings.city(context)
    val asr = PrayerSettings.asr(context)
    val today = Calendar.getInstance()
    val times = calculatePrayerTimes(
        city, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH), asr,
    )

    var masjid by remember { mutableStateOf(MasjidTimings.activeIndex(context)) }
    val masjids = MasjidTimings.masjids(context)

    SectionCard {
        SectionHeader(title = stringResource(R.string.prayer_title, city.name))
        if (masjids.isNotEmpty()) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                masjids.forEach { (slot, name) ->
                    AppChip(
                        selected = slot == masjid,
                        onClick = {
                            masjid = slot
                            MasjidTimings.setActive(context, slot)
                        },
                        label = { Text(name) },
                    )
                }
            }
        }
        // Each row shows the masjid's own time when it is entered, and the calculated time otherwise.
        // Subuh Sadiq is the start of the dawn; Fajr is the prayer. Sunset is the moment Maghrib begins.
        PrayerRow(stringResource(R.string.prayer_subuh_sadiq), shownTime(context, "subuh_sadiq", times.fajr, masjid), null, masjid)
        PrayerRow(stringResource(R.string.prayer_fajr), shownTime(context, "fajr", times.fajr, masjid), "fajr", masjid)
        PrayerRow(stringResource(R.string.prayer_sunrise), shownTime(context, "sunrise", times.sunrise, masjid), null, masjid)
        PrayerRow(stringResource(R.string.prayer_zuhr), shownTime(context, "zuhr", times.zuhr, masjid), "zuhr", masjid)
        PrayerRow(stringResource(R.string.prayer_asr), shownTime(context, "asr", times.asr, masjid), "asr", masjid)
        PrayerRow(stringResource(R.string.prayer_sunset), shownTime(context, "sunset", times.maghrib, masjid), null, masjid)
        PrayerRow(stringResource(R.string.prayer_maghrib), shownTime(context, "maghrib", times.maghrib, masjid), "maghrib", masjid)
        PrayerRow(stringResource(R.string.prayer_isha), shownTime(context, "isha", times.isha, masjid), "isha", masjid)
        // Jumah is on Friday only. Without its own time it is shown at the time of Zuhr.
        if (today.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
            PrayerRow(stringResource(R.string.prayer_jumah), shownTime(context, "jumah", times.zuhr, masjid), "jumah", masjid)
        }
    }
}

/** The masjid's time for a row if it is entered, otherwise the calculated time. */
private fun shownTime(context: Context, name: String, calculated: Double, masjid: Int): Double =
    MasjidTimings.manualMinutes(context, name, masjid) ?: calculated

/** One prayer. [prayer] is its key in [MasjidTimings], or null when the masjid has no time for it (sunrise). */
@Composable
private fun PrayerRow(name: String, minutes: Double, prayer: String?, masjid: Int) {
    val context = LocalContext.current
    val adhan = prayer?.let { MasjidTimings.currentValue(context, "adhan", it, masjid) }.orEmpty()
    val iqama = prayer?.let { MasjidTimings.currentValue(context, "iqama", it, masjid) }.orEmpty()
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text(formatPrayerTime(minutes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        if (adhan.isNotBlank() || iqama.isNotBlank()) {
            Text(
                stringResource(
                    R.string.prayer_masjid_line,
                    adhan.ifBlank { "-" },
                    iqama.ifBlank { "-" },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Settings: choose the city and the Asr method. */
@Composable
fun PrayerSettingsSection() {
    val context = LocalContext.current
    var city by remember { mutableStateOf(PrayerSettings.city(context)) }
    var asr by remember { mutableStateOf(PrayerSettings.asr(context)) }
    var reminders by remember { mutableStateOf(PrayerSettings.remindersOn(context)) }
    var active by remember { mutableStateOf(MasjidTimings.activeIndex(context)) }
    var masjids by remember { mutableStateOf(MasjidTimings.masjids(context)) }
    var newName by remember { mutableStateOf("") }
    var editingMasjid by remember { mutableStateOf<Int?>(null) }
    val masjidLimit = stringResource(R.string.prayer_masjid_limit, MasjidTimings.MAX_MASJIDS)

    SectionCard {
        SectionHeader(title = stringResource(R.string.prayer_settings_title))
        Text(stringResource(R.string.prayer_settings_desc), style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.prayer_reminders),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = reminders,
                onCheckedChange = {
                    reminders = it
                    PrayerSettings.saveRemindersOn(context, it)
                },
            )
        }
        CategorySelector(
            label = stringResource(R.string.prayer_city),
            options = cities.map { it.name },
            selected = city.name,
            onSelect = { name ->
                val picked: City = cities.first { it.name == name }
                city = picked
                PrayerSettings.saveCity(context, picked)
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.prayer_asr_label), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppChip(
                    selected = asr == AsrMethod.STANDARD,
                    onClick = {
                        asr = AsrMethod.STANDARD
                        PrayerSettings.saveAsr(context, asr)
                    },
                    label = { Text(stringResource(R.string.prayer_asr_standard)) },
                )
                AppChip(
                    selected = asr == AsrMethod.HANAFI,
                    onClick = {
                        asr = AsrMethod.HANAFI
                        PrayerSettings.saveAsr(context, asr)
                    },
                    label = { Text(stringResource(R.string.prayer_asr_hanafi)) },
                )
            }
        }
    }

    SectionCard {
        SectionHeader(title = stringResource(R.string.prayer_masjids_title))
        Text(stringResource(R.string.prayer_masjids_desc), style = MaterialTheme.typography.bodySmall)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            masjids.forEach { (slot, name) ->
                AppChip(
                    selected = slot == active,
                    onClick = {
                        active = slot
                        MasjidTimings.setActive(context, slot)
                    },
                    label = { Text(name) },
                )
            }
        }
        if (masjids.isEmpty()) {
            Text(stringResource(R.string.prayer_masjids_none), style = MaterialTheme.typography.bodySmall)
        }
        if (masjids.size < MasjidTimings.MAX_MASJIDS) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.prayer_masjid_name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    enabled = newName.isNotBlank(),
                    onClick = {
                        val slot = MasjidTimings.add(context, newName)
                        if (slot != null) {
                            active = MasjidTimings.activeIndex(context)
                            masjids = MasjidTimings.masjids(context)
                            newName = ""
                        }
                    },
                ) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.prayer_masjid_add)) }
            }
        } else {
            Text(masjidLimit, style = MaterialTheme.typography.bodySmall)
        }
        if (masjids.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { editingMasjid = active }) {
                    Text(stringResource(R.string.prayer_masjid_button))
                }
                IconButton(onClick = {
                    MasjidTimings.remove(context, active)
                    active = MasjidTimings.activeIndex(context)
                    masjids = MasjidTimings.masjids(context)
                }) { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.prayer_masjid_remove)) }
            }
        }
    }

    editingMasjid?.let { masjid ->
        MasjidTimingsDialog(masjid = masjid, onDismiss = { editingMasjid = null })
    }
}

/** Enter the adhan and iqamah times the masjid announces, one prayer at a time. */
@Composable
private fun MasjidTimingsDialog(masjid: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val values = remember {
        mutableStateMapOf<String, String>().apply {
            MasjidTimings.timeNames.forEach { name ->
                put(MasjidTimings.key("time", name, masjid), MasjidTimings.get(context, "time", name, masjid))
            }
            MasjidTimings.prayers.forEach { prayer ->
                put(MasjidTimings.key("adhan", prayer, masjid), MasjidTimings.get(context, "adhan", prayer, masjid))
                put(MasjidTimings.key("iqama", prayer, masjid), MasjidTimings.get(context, "iqama", prayer, masjid))
            }
        }
    }
    val (initialFrom, initialTo) = MasjidTimings.rangeText(context, masjid)
    var fromText by remember { mutableStateOf(initialFrom) }
    var toText by remember { mutableStateOf(initialTo) }
    var error by remember { mutableStateOf(false) }
    var rangeError by remember { mutableStateOf(false) }
    var photoVersion by remember { mutableStateOf(0) }
    var photoError by remember { mutableStateOf(false) }
    var viewingPhoto by remember { mutableStateOf(false) }
    val hasPhoto = remember(photoVersion) { MasjidTimings.hasPhoto(context, masjid) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoError = !MasjidTimings.savePhoto(context, masjid, uri)
            photoVersion++
        }
    }
    val timeError = stringResource(R.string.prayer_masjid_error)
    val rangeMessage = stringResource(R.string.prayer_range_error)
    val adhanLabel = stringResource(R.string.prayer_masjid_adhan)
    val iqamaLabel = stringResource(R.string.prayer_masjid_iqama)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prayer_masjid_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.prayer_masjid_desc), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.prayer_range_title), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.prayer_range_desc), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fromText,
                        onValueChange = { fromText = formatDateTyping(it) },
                        label = { Text(stringResource(R.string.prayer_range_from)) },
                        placeholder = { Text(stringResource(R.string.mastoorat_date_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = toText,
                        onValueChange = { toText = formatDateTyping(it) },
                        label = { Text(stringResource(R.string.prayer_range_to)) },
                        placeholder = { Text(stringResource(R.string.mastoorat_date_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rangeError) {
                    Text(rangeMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(stringResource(R.string.prayer_photo_title), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickPhoto.launch("image/*") }) { Text(stringResource(R.string.prayer_photo_add)) }
                    if (hasPhoto) {
                        TextButton(onClick = { viewingPhoto = true }) { Text(stringResource(R.string.prayer_photo_view)) }
                        TextButton(onClick = {
                            MasjidTimings.deletePhoto(context, masjid)
                            photoVersion++
                        }) { Text(stringResource(R.string.prayer_photo_remove)) }
                    }
                }
                if (photoError) {
                    Text(stringResource(R.string.prayer_photo_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Text(stringResource(R.string.prayer_override_title), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.prayer_override_desc), style = MaterialTheme.typography.bodySmall)
                MasjidTimings.timeNames.forEach { name ->
                    val timeKey = MasjidTimings.key("time", name, masjid)
                    OutlinedTextField(
                        value = values[timeKey].orEmpty(),
                        onValueChange = { values[timeKey] = formatTimeTyping(it) },
                        label = { Text(stringResource(timeLabelRes(name))) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MasjidTimings.prayers.forEach { prayer ->
                    val adhanKey = MasjidTimings.key("adhan", prayer, masjid)
                    val iqamaKey = MasjidTimings.key("iqama", prayer, masjid)
                    Text(stringResource(prayerNameRes(prayer)), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = values[adhanKey].orEmpty(),
                            onValueChange = { values[adhanKey] = formatTimeTyping(it) },
                            label = { Text(adhanLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = values[iqamaKey].orEmpty(),
                            onValueChange = { values[iqamaKey] = formatTimeTyping(it) },
                            label = { Text(iqamaLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (error) {
                    Text(timeError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val all = values.toMap()
                val rangeOk = MasjidTimings.saveRange(context, masjid, fromText, toText)
                rangeError = !rangeOk
                if (all.values.any { it.isNotBlank() && !MasjidTimings.isValid(it) }) {
                    error = true
                } else if (rangeOk) {
                    MasjidTimings.saveAll(context, masjid, all)
                    onDismiss()
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (viewingPhoto) {
        val photo = remember(photoVersion) {
            BitmapFactory.decodeFile(MasjidTimings.photoFile(context, masjid).absolutePath)?.asImageBitmap()
        }
        AlertDialog(
            onDismissRequest = { viewingPhoto = false },
            title = { Text(stringResource(R.string.prayer_photo_title)) },
            text = {
                if (photo != null) {
                    Image(bitmap = photo, contentDescription = stringResource(R.string.prayer_photo_title), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { viewingPhoto = false }) { Text(stringResource(R.string.prayer_photo_close)) } },
        )
    }
}

/** The label of a row the masjid can give its own time for. */
private fun timeLabelRes(name: String): Int = when (name) {
    "subuh_sadiq" -> R.string.prayer_subuh_sadiq
    "sunrise" -> R.string.prayer_sunrise
    "sunset" -> R.string.prayer_sunset
    else -> prayerNameRes(name)
}

private fun prayerNameRes(prayer: String): Int = when (prayer) {
    "fajr" -> R.string.prayer_fajr
    "zuhr" -> R.string.prayer_zuhr
    "asr" -> R.string.prayer_asr
    "maghrib" -> R.string.prayer_maghrib
    "jumah" -> R.string.prayer_jumah
    else -> R.string.prayer_isha
}
