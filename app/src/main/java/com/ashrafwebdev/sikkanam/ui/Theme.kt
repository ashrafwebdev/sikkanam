package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// Deep green for the brand, gold for accents. Green and red mark income and expenses.
private val Green700 = Color(0xFF0B6B4F)
private val Green100 = Color(0xFFCDEFE1)
private val Gold600 = Color(0xFF9A7300)
private val Gold100 = Color(0xFFFCEFC7)

private val LightColors = lightColorScheme(
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = Green100,
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Gold600,
    onSecondary = Color.White,
    secondaryContainer = Gold100,
    onSecondaryContainer = Color(0xFF261900),
    background = Color(0xFFF7F9F6),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6EDE8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD6B3),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Green700,
    onPrimaryContainer = Green100,
    secondary = Color(0xFFE8C36A),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5A4300),
    onSecondaryContainer = Gold100,
)

/** Money colors. Income is green and expense is red in both themes. */
val IncomeColor = Color(0xFF2E7D32)
val ExpenseColor = Color(0xFFC62828)

@Composable
fun SikkanamTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    remember { TextSizeSetting.load(context) }
    val textSize by TextSizeSetting.size.collectAsState()
    val base = LocalDensity.current

    // Scales every sp text size in the app. The phone's own font size is already in base.fontScale.
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * textSize.factor)) {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
            content = content,
        )
    }
}
