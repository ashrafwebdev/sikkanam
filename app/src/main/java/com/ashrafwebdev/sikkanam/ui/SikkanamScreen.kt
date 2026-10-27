package com.ashrafwebdev.sikkanam.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ashrafwebdev.sikkanam.update.UpdateBanner
import com.ashrafwebdev.sikkanam.BuildConfig
import com.ashrafwebdev.sikkanam.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Tab(@StringRes val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Default.Home),
    BUDGETS(R.string.tab_budgets, Icons.Default.DateRange),
    CHARITY(R.string.tab_charity, Icons.Default.Favorite),
    TRIPS(R.string.tab_trips, Icons.Default.Person),
    ZAKAT(R.string.tab_zakat, Icons.Default.Info),
    SETTINGS(R.string.tab_settings, Icons.Default.Settings),
}

@Composable
fun SikkanamScreen(viewModel: FinanceViewModel = viewModel()) {
    var selected by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            BrandHeader(title = stringResource(Tab.entries[selected].labelRes))
            if (BuildConfig.SELF_UPDATE) UpdateBanner(LocalContext.current)

            val contentModifier = Modifier.weight(1f).padding(top = 12.dp)
            when (Tab.entries[selected]) {
                Tab.HOME -> HomeScreen(viewModel, contentModifier)
                Tab.BUDGETS -> BudgetsScreen(viewModel, contentModifier)
                Tab.CHARITY -> CharityScreen(viewModel, contentModifier)
                Tab.TRIPS -> TripsScreen(viewModel, contentModifier)
                Tab.ZAKAT -> ZakatScreen(contentModifier)
                Tab.SETTINGS -> SettingsScreen(viewModel, contentModifier)
            }
        }
    }
}

/** Top band with the Tamil logo, the app name, and the current section. */
@Composable
private fun BrandHeader(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.brand_tamil), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.brand_name), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
    }
}
