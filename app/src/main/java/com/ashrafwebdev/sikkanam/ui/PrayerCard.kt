package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
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

    SectionCard {
        SectionHeader(title = stringResource(R.string.prayer_title, city.name))
        PrayerRow(stringResource(R.string.prayer_fajr), times.fajr)
        PrayerRow(stringResource(R.string.prayer_sunrise), times.sunrise)
        PrayerRow(stringResource(R.string.prayer_zuhr), times.zuhr)
        PrayerRow(stringResource(R.string.prayer_asr), times.asr)
        PrayerRow(stringResource(R.string.prayer_maghrib), times.maghrib)
        PrayerRow(stringResource(R.string.prayer_isha), times.isha)
    }
}

@Composable
private fun PrayerRow(name: String, minutes: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(formatPrayerTime(minutes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Settings: choose the city and the Asr method. */
@Composable
fun PrayerSettingsSection() {
    val context = LocalContext.current
    var city by remember { mutableStateOf(PrayerSettings.city(context)) }
    var asr by remember { mutableStateOf(PrayerSettings.asr(context)) }
    var reminders by remember { mutableStateOf(PrayerSettings.remindersOn(context)) }

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
                FilterChip(
                    selected = asr == AsrMethod.STANDARD,
                    onClick = {
                        asr = AsrMethod.STANDARD
                        PrayerSettings.saveAsr(context, asr)
                    },
                    label = { Text(stringResource(R.string.prayer_asr_standard)) },
                )
                FilterChip(
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
}
