package com.ashrafwebdev.sikkanam.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Two-finger pinch. [onZoom] gets the change since the last call (above 1 is bigger). One finger is
 * left alone, so the list under it still scrolls.
 */
@Composable
fun Modifier.pinchToZoom(onZoom: (Float) -> Unit): Modifier {
    val currentOnZoom by rememberUpdatedState(onZoom)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                // Seen before the scrolling list, so a pinch zooms instead of scrolling.
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) currentOnZoom(zoom)
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

/** Hides the status and navigation bars while on screen. A swipe from the edge shows them for a moment. */
@Composable
fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
