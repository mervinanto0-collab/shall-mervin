package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TriageEmergencyRed
import com.example.ui.theme.TriageSafeGreen
import com.example.ui.theme.TriageUrgentOrange
import com.example.ui.theme.VitalHeartRate
import com.example.ui.theme.VitalOxygen
import com.example.ui.theme.VitalTemperature
import java.util.Locale

/**
 * Health vital sign status categories with associated color codes.
 */
enum class VitalStatus(val label: String, val color: Color) {
    OPTIMAL("Optimal", TriageSafeGreen),
    NORMAL("Normal", TriageSafeGreen),
    WARNING("Caution", TriageUrgentOrange),
    CRITICAL("Alert", TriageEmergencyRed)
}

/**
 * A composable function that displays live heart rate, oxygen, and temperature vitals,
 * using cards with icons and color-coded status indicators.
 *
 * @param heartRate Current heart rate in beats per minute (BPM).
 * @param spo2 Current blood oxygen saturation percentage (%).
 * @param temperatureF Current body temperature in degrees Fahrenheit (°F).
 * @param modifier Optional [Modifier] for configuring layout and styling.
 */
@Composable
fun LiveVitalsDisplay(
    heartRate: Int,
    spo2: Int,
    temperatureF: Float,
    modifier: Modifier = Modifier
) {
    // Determine clinical status for Heart Rate
    val hrStatus = when {
        heartRate >= 120 || heartRate < 45 -> VitalStatus.CRITICAL
        heartRate > 100 || heartRate < 55 -> VitalStatus.WARNING
        heartRate in 60..85 -> VitalStatus.OPTIMAL
        else -> VitalStatus.NORMAL
    }

    // Determine clinical status for Oxygen Saturation (SpO2)
    val o2Status = when {
        spo2 < 90 -> VitalStatus.CRITICAL
        spo2 < 95 -> VitalStatus.WARNING
        spo2 >= 97 -> VitalStatus.OPTIMAL
        else -> VitalStatus.NORMAL
    }

    // Determine clinical status for Body Temperature (°F)
    val tempStatus = when {
        temperatureF >= 101.5f || temperatureF < 95.5f -> VitalStatus.CRITICAL
        temperatureF >= 99.5f || temperatureF < 97.0f -> VitalStatus.WARNING
        temperatureF in 97.8f..98.9f -> VitalStatus.OPTIMAL
        else -> VitalStatus.NORMAL
    }

    // Subtle rhythmic heartbeat pulse animation for the heart icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_vitals_display"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section title with live pulse beacon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TriageSafeGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Live Physiological Vitals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Continuous Telemetry",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Live Vital Cards: Heart Rate, Oxygen, Temperature
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Heart Rate Card
            LiveVitalCardItem(
                title = "Heart Rate",
                value = "$heartRate",
                unit = "BPM",
                targetRange = "60-100",
                icon = Icons.Default.Favorite,
                accentColor = VitalHeartRate,
                status = hrStatus,
                iconScale = pulseScale,
                modifier = Modifier.weight(1f),
                testTag = "live_vital_heart_rate_card"
            )

            // 2. Oxygen Saturation Card
            LiveVitalCardItem(
                title = "Oxygen",
                value = "$spo2%",
                unit = "SpO2",
                targetRange = "95-100%",
                icon = Icons.Default.Air,
                accentColor = VitalOxygen,
                status = o2Status,
                modifier = Modifier.weight(1f),
                testTag = "live_vital_oxygen_card"
            )

            // 3. Body Temperature Card
            val tempFormatted = String.format(Locale.getDefault(), "%.1f°", temperatureF)
            LiveVitalCardItem(
                title = "Temp",
                value = tempFormatted,
                unit = "F",
                targetRange = "97.8-99.1°",
                icon = Icons.Default.Thermostat,
                accentColor = VitalTemperature,
                status = tempStatus,
                modifier = Modifier.weight(1f),
                testTag = "live_vital_temp_card"
            )
        }
    }
}

/**
 * Individual card displaying a single live vital sign with icon and color-coded status badge.
 */
@Composable
fun LiveVitalCardItem(
    title: String,
    value: String,
    unit: String,
    targetRange: String,
    icon: ImageVector,
    accentColor: Color,
    status: VitalStatus,
    modifier: Modifier = Modifier,
    iconScale: Float = 1.0f,
    testTag: String = "vital_card_item"
) {
    Card(
        modifier = modifier
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Icon + Color-Coded Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier
                            .size(18.dp)
                            .scale(iconScale)
                    )
                }

                // Color-Coded Status Indicator Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(status.color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(status.color)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = status.label,
                            color = status.color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vital Title
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            // Large Value & Unit
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Target Normal Range Label
            Text(
                text = "Target: $targetRange",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LiveVitalsDisplayPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            LiveVitalsDisplay(
                heartRate = 72,
                spo2 = 98,
                temperatureF = 98.6f
            )
        }
    }
}
