package com.ashrafwebdev.sikkanam.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.prayer.PrayerSettings
import com.ashrafwebdev.sikkanam.prayer.qiblaBearing
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** How close (in degrees) the phone must point to the Qibla before it says so. */
private const val ON_TARGET_DEGREES = 5.0

/**
 * Qibla compass for the chosen city. A dial turns with the phone, so the N mark always shows true north.
 * The Kaaba marker shows where the Qibla is, and the text says which way to turn. It uses the phone's
 * rotation sensor, so it needs no internet and no location permission.
 */
@Composable
fun QiblaCard() {
    val context = LocalContext.current
    val city = PrayerSettings.city(context)
    val bearing = qiblaBearing(city.lat, city.lon)
    var heading by remember { mutableStateOf<Float?>(null) }
    val sensorManager = context.getSystemService(SensorManager::class.java)
    val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    DisposableEffect(sensor) {
        if (sensorManager == null || sensor == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                private val rotation = FloatArray(9)
                private val orientation = FloatArray(3)

                override fun onSensorChanged(event: SensorEvent) {
                    SensorManager.getRotationMatrixFromVector(rotation, event.values)
                    SensorManager.getOrientation(rotation, orientation)
                    heading = ((Math.toDegrees(orientation[0].toDouble()) + 360) % 360).toFloat()
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    SectionCard {
        SectionHeader(title = stringResource(R.string.qibla_title))
        if (sensor == null) {
            Text(stringResource(R.string.qibla_no_sensor), style = MaterialTheme.typography.bodySmall)
            return@SectionCard
        }
        val current = heading
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (current == null) {
                Text(stringResource(R.string.qibla_waiting), style = MaterialTheme.typography.bodySmall)
                return@Column
            }

            // How far the Qibla is from the direction the top of the phone points, in -180..180.
            // Positive means turn right, negative means turn left.
            val diff = normalize(bearing - current)
            val onTarget = abs(diff) <= ON_TARGET_DEGREES
            val dialColor = MaterialTheme.colorScheme.onSurfaceVariant
            val kaabaColor = if (onTarget) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.size(220.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val radius = size.width * 0.42f

                // The dial turns with the phone, so N always points to true north.
                val northAngle = -current.toDouble()
                drawCircle(color = dialColor, radius = radius, center = Offset(cx, cy), style = Stroke(width = 3f))
                for (deg in 0 until 360 step 30) {
                    val a = Math.toRadians((deg - current).toDouble())
                    val inner = radius * 0.86f
                    drawLine(
                        color = dialColor,
                        start = Offset(cx + inner * sin(a).toFloat(), cy - inner * cos(a).toFloat()),
                        end = Offset(cx + radius * sin(a).toFloat(), cy - radius * cos(a).toFloat()),
                        strokeWidth = 2f,
                    )
                }
                val nAngle = Math.toRadians(northAngle)
                drawCircle(
                    color = Color(0xFFC62828),
                    radius = 9f,
                    center = Offset(cx + radius * 0.7f * sin(nAngle).toFloat(), cy - radius * 0.7f * cos(nAngle).toFloat()),
                )

                // The Kaaba marker: where the Qibla is, relative to the top of the phone.
                val qAngle = Math.toRadians(diff)
                val qx = cx + radius * sin(qAngle).toFloat()
                val qy = cy - radius * cos(qAngle).toFloat()
                drawLine(color = kaabaColor, start = Offset(cx, cy), end = Offset(qx, qy), strokeWidth = 4f)
                val box = 22f
                drawRect(color = kaabaColor, topLeft = Offset(qx - box / 2, qy - box / 2), size = androidx.compose.ui.geometry.Size(box, box))

                // The fixed pointer at the top: the front of the phone.
                drawCircle(color = dialColor, radius = 6f, center = Offset(cx, cy))
            }

            val guidance = when {
                onTarget -> stringResource(R.string.qibla_facing)
                diff > 0 -> stringResource(R.string.qibla_turn_right, abs(diff).toInt())
                else -> stringResource(R.string.qibla_turn_left, abs(diff).toInt())
            }
            Text(
                guidance,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (onTarget) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.qibla_degrees, bearing.toInt()),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(stringResource(R.string.qibla_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Brings an angle into -180..180. */
private fun normalize(angle: Double): Double {
    var a = (angle % 360 + 540) % 360 - 180
    if (a == -180.0) a = 180.0
    return a
}
