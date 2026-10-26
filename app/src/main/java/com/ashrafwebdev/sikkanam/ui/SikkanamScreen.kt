package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    BUDGETS("Budgets", Icons.Default.DateRange),
    CHARITY("Charity", Icons.Default.Favorite),
    ZAKAT("Zakat", Icons.Default.Info),
    SETTINGS("Settings", Icons.Default.Settings),
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
                        icon = { androidx.compose.material3.Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (Tab.entries[selected]) {
            Tab.HOME -> HomeScreen(viewModel, modifier)
            Tab.BUDGETS -> BudgetsScreen(viewModel, modifier)
            Tab.CHARITY -> CharityScreen(viewModel, modifier)
            Tab.ZAKAT -> ZakatScreen(modifier)
            Tab.SETTINGS -> SettingsScreen(viewModel, modifier)
        }
    }
}
