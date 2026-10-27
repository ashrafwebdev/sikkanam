package com.ashrafwebdev.sikkanam.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashrafwebdev.sikkanam.BuildConfig
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.update.UpdateBanner

/** The bottom-bar pages. Islamic and Jamaat pages are hidden when switched off in Settings. */
private enum class Tab(@StringRes val labelRes: Int, val icon: ImageVector) {
    MONEY(R.string.tab_money, Icons.Default.Home),
    BUDGETS(R.string.tab_budgets, Icons.Default.DateRange),
    ISLAMIC(R.string.tab_islamic, Icons.Default.Info),
    JAMAAT(R.string.tab_jamaat, Icons.Default.Person),
    DIRECTORY(R.string.tab_directory, Icons.Default.LocationOn),
    SETTINGS(R.string.tab_settings, Icons.Default.Settings),
}

@Composable
fun SikkanamScreen(viewModel: FinanceViewModel = viewModel()) {
    val context = LocalContext.current
    val tabs = Tab.entries.filter { tab ->
        when (tab) {
            Tab.ISLAMIC -> islamicPagesOn(context)
            Tab.JAMAAT -> jamaatPagesOn(context)
            else -> true
        }
    }
    var selected by remember { mutableStateOf(Tab.MONEY) }
    val current = if (selected in tabs) selected else Tab.MONEY

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab,
                        onClick = { selected = tab },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            BrandHeader(title = stringResource(current.labelRes))
            if (BuildConfig.SELF_UPDATE) UpdateBanner(context)

            val contentModifier = Modifier.weight(1f).padding(top = 12.dp)
            when (current) {
                Tab.MONEY -> HomeScreen(viewModel, contentModifier)
                Tab.BUDGETS -> BudgetsScreen(viewModel, contentModifier)
                Tab.ISLAMIC -> IslamicScreen(viewModel, contentModifier)
                Tab.JAMAAT -> TripsScreen(viewModel, contentModifier)
                // Back from the directory goes to Money, since it is a tab now.
                Tab.DIRECTORY -> DirectoryPage(viewModel, onBack = { selected = Tab.MONEY }, modifier = contentModifier)
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
