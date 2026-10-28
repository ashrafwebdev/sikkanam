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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    TRAVEL(R.string.tab_travel, Icons.AutoMirrored.Filled.Send),
    NOTES(R.string.tab_notes, Icons.Default.Edit),
    HELP(R.string.tab_help, Icons.Default.Search),
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
            TabBar(tabs = tabs, current = current, onSelect = { selected = it })
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
                Tab.TRAVEL -> TravelScreen(viewModel, contentModifier)
                Tab.NOTES -> NotesScreen(viewModel, contentModifier)
                Tab.HELP -> HelpScreen(contentModifier)
                Tab.SETTINGS -> SettingsScreen(viewModel, contentModifier)
            }
        }
    }
}

/** Bottom bar that scrolls sideways, so new pages can be added without squeezing the existing ones. */
@Composable
private fun TabBar(tabs: List<Tab>, current: Tab, onSelect: (Tab) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            tabs.forEach { tab ->
                val selected = current == tab
                val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 8.dp),
                ) {
                    Icon(tab.icon, contentDescription = null, tint = color)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
