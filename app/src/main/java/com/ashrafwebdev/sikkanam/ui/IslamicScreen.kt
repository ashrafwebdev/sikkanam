package com.ashrafwebdev.sikkanam.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R

/** The pages inside the Islamic tab. */
private enum class IslamicPage(@StringRes val labelRes: Int) {
    PRAYER(R.string.islamic_prayer),
    ZAKAT(R.string.tab_zakat),
    CHARITY(R.string.tab_charity),
    LEARN(R.string.islamic_learn),
}

/**
 * The Islamic tab: prayer times, Qibla, Islamic dates and the daily dhikr and Quran tracker; then zakat
 * (with inheritance), charity and purification, and learning. Each page has its own chip above it.
 */
@Composable
fun IslamicScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableStateOf(IslamicPage.PRAYER) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IslamicPage.entries.forEach { option ->
                FilterChip(
                    selected = page == option,
                    onClick = { page = option },
                    label = { Text(stringResource(option.labelRes)) },
                )
            }
        }
        Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            when (page) {
                IslamicPage.PRAYER -> Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrayerCard()
                    QiblaCard()
                    IslamicDatesCard()
                    IbadahCard()
                }
                IslamicPage.ZAKAT -> ZakatScreen(Modifier.fillMaxSize())
                IslamicPage.CHARITY -> CharityScreen(vm, Modifier.fillMaxSize())
                IslamicPage.LEARN -> Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LearnSection()
                }
            }
        }
    }
}
