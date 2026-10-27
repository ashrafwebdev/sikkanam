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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.prayer.PrayerSettings
import com.ashrafwebdev.sikkanam.prayer.qiblaBearing

/**
 * Qibla direction for the chosen city. The arrow turns with the phone and points at the Kaaba.
 * It uses the phone's rotation sensor, so it needs no internet and no location permission.
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
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val current = heading
            if (current == null) {
                Text(stringResource(R.string.qibla_waiting), style = MaterialTheme.typography.bodySmall)
            } else {
                val arrowAngle = (bearing - current).toFloat()
                val arrowColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(140.dp)) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val half = size.width * 0.18f
                    val top = size.height * 0.05f
                    val bottom = size.height * 0.95f
                    rotate(degrees = arrowAngle, pivot = Offset(cx, cy)) {
                        val path = Path().apply {
                            moveTo(cx, top)
                            lineTo(cx - half, bottom)
                            lineTo(cx, bottom - size.height * 0.2f)
                            lineTo(cx + half, bottom)
                            close()
                        }
                        drawPath(path, arrowColor)
                    }
                }
            }
            Text(
                stringResource(R.string.qibla_degrees, bearing.toInt()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(stringResource(R.string.qibla_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}
