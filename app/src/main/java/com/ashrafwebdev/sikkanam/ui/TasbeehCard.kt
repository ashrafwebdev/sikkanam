package com.ashrafwebdev.sikkanam.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashrafwebdev.sikkanam.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS = "tasbeeh"
private const val KEY_COUNT = "count"
private const val KEY_TARGET = "target"
private const val KEY_SOUND = "sound"
private const val KEY_VIBRATE = "vibrate"
private const val MAX_COUNT = 99_999

/** Cycle targets offered, like the 33/99/100 settings on a hand-held counter. Zero means no target. */
private val targets = listOf(33, 99, 100, 0)

private fun Context.tasbeehPrefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
private fun todayKey(): String = "today_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

private fun Context.vibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

private fun Vibrator.buzz(millis: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrate(millis)
    }
}

/**
 * A digital tasbeeh, drawn like the hand-held electronic counter: an LCD display, one big count
 * button, and a small reset button. Each press clicks and buzzes; finishing a round buzzes longer.
 * The count stays until it is reset, like the real device. Today's total is also kept.
 */
@Composable
fun TasbeehCard() {
    val context = LocalContext.current
    val prefs = remember { context.tasbeehPrefs() }
    var count by remember { mutableIntStateOf(prefs.getInt(KEY_COUNT, 0)) }
    var today by remember { mutableIntStateOf(prefs.getInt(todayKey(), 0)) }
    var target by remember { mutableIntStateOf(prefs.getInt(KEY_TARGET, 33)) }
    var sound by remember { mutableStateOf(prefs.getBoolean(KEY_SOUND, true)) }
    var vibrate by remember { mutableStateOf(prefs.getBoolean(KEY_VIBRATE, true)) }
    var confirmReset by remember { mutableStateOf(false) }

    val vibrator = remember { context.vibrator() }
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_SYSTEM, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }

    fun press() {
        count = (count + 1).coerceAtMost(MAX_COUNT)
        today += 1
        prefs.edit().putInt(KEY_COUNT, count).putInt(todayKey(), today).apply()
        val roundDone = target > 0 && count % target == 0
        if (vibrate) vibrator?.buzz(if (roundDone) 400 else 25)
        if (sound) tone?.startTone(if (roundDone) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP, if (roundDone) 250 else 40)
    }

    SectionCard {
        SectionHeader(title = stringResource(R.string.tasbeeh_title))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TasbeehDevice(
                count = count,
                target = target,
                onCount = ::press,
                onReset = { confirmReset = true },
            )
        }
        Text(stringResource(R.string.ibadah_dhikr, today), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.tasbeeh_target), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            targets.forEach { option ->
                AppChip(
                    selected = target == option,
                    onClick = {
                        target = option
                        prefs.edit().putInt(KEY_TARGET, option).apply()
                    },
                    label = { Text(if (option == 0) stringResource(R.string.tasbeeh_no_target) else option.toString()) },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sound, onCheckedChange = {
                sound = it
                prefs.edit().putBoolean(KEY_SOUND, it).apply()
            })
            Text(stringResource(R.string.tasbeeh_sound))
            Spacer(Modifier.width(12.dp))
            Checkbox(checked = vibrate, onCheckedChange = {
                vibrate = it
                prefs.edit().putBoolean(KEY_VIBRATE, it).apply()
            })
            Text(stringResource(R.string.tasbeeh_vibrate))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.tasbeeh_reset_title)) },
            text = { Text(stringResource(R.string.tasbeeh_reset_body, count)) },
            confirmButton = {
                TextButton(onClick = {
                    count = 0
                    prefs.edit().putInt(KEY_COUNT, 0).apply()
                    if (vibrate) vibrator?.buzz(60)
                    confirmReset = false
                }) { Text(stringResource(R.string.ibadah_reset)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private val LcdGreen = Color(0xFFA9B98C)
private val LcdInk = Color(0xFF1E2614)
private val LcdGhost = Color(0x1A1E2614)

@Composable
private fun TasbeehDevice(count: Int, target: Int, onCount: () -> Unit, onReset: () -> Unit) {
    val bodyShape = RoundedCornerShape(topStart = 110.dp, topEnd = 110.dp, bottomStart = 130.dp, bottomEnd = 130.dp)
    Column(
        modifier = Modifier
            .width(240.dp)
            .shadow(10.dp, bodyShape)
            .background(
                Brush.linearGradient(listOf(Color(0xFF3C6E5A), Color(0xFF1F4637), Color(0xFF14302A))),
                bodyShape,
            )
            .border(BorderStroke(2.dp, Color(0x33FFFFFF)), bodyShape)
            .padding(top = 44.dp, bottom = 36.dp, start = 28.dp, end = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LcdDisplay(count = count, target = target)
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            SmallResetButton(onReset)
        }
        Spacer(Modifier.height(6.dp))
        CountButton(onCount)
    }
}

@Composable
private fun LcdDisplay(count: Int, target: Int) {
    val round = if (target > 0) count / target else 0
    val inRound = if (target > 0) count % target else count
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0E1A15), RoundedCornerShape(12.dp))
            .padding(6.dp)
            .background(
                Brush.verticalGradient(listOf(LcdGreen, Color(0xFF97A87A))),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                if (target > 0) "${stringResource(R.string.tasbeeh_lcd_round)} $round" else "",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = LcdInk,
                modifier = Modifier.weight(1f),
            )
            if (target > 0) {
                Text("$inRound/$target", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = LcdInk)
            }
        }
        // Faint "88888" behind the digits, like the unlit segments of a real LCD.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text("88888", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 44.sp, color = LcdGhost)
            Text(
                count.toString().padStart(5, ' '),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = LcdInk,
            )
        }
    }
}

@Composable
private fun SmallResetButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .scale(if (pressed) 0.88f else 1f)
                .shadow(if (pressed) 1.dp else 4.dp, CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFE0E0E0), Color(0xFF8A8A8A))), CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        )
        Text(stringResource(R.string.tasbeeh_lcd_reset), fontSize = 9.sp, color = Color(0xCCFFFFFF))
    }
}

@Composable
private fun CountButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "press")
    val elevation by animateDpAsState(if (pressed) 2.dp else 12.dp, label = "lift")
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .shadow(elevation, CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFF5F5F5), Color(0xFFBDBDBD), Color(0xFF7D7D7D))), CircleShape)
            .border(BorderStroke(3.dp, Color(0x55000000)), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.tasbeeh_count_button),
            color = Color(0xFF424242),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
