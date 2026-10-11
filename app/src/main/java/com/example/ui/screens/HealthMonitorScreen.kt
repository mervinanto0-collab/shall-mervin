package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SensorDevice
import com.example.data.model.VitalsRecord
import com.example.engine.LiveVitalsState
import com.example.engine.SimulationProfile
import com.example.ui.components.LiveEcgWaveform
import com.example.ui.components.LiveVitalsDisplay
import com.example.ui.components.MetricStatus
import com.example.ui.components.VitalMetricCard
import com.example.ui.theme.TriageEmergencyRed
import com.example.ui.theme.TriageSafeGreen
import com.example.ui.theme.TriageUrgentOrange
import com.example.ui.theme.VitalBloodPressure
import com.example.ui.theme.VitalHeartRate
import com.example.ui.theme.VitalOxygen
import com.example.ui.theme.VitalRespiration
import com.example.ui.theme.VitalTemperature
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HealthMonitorScreen(
    liveVitals: LiveVitalsState,
    ecgPoints: List<Float>,
    vitalsHistory: List<VitalsRecord>,
    onScanNow: () -> Unit,
    onSimulationProfileChange: (SimulationProfile) -> Unit,
    onConnectDoctor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("health_monitor_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Tablet Station Header Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(TriageSafeGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PATIENT TABLET STATION ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Continuous Health Tele-Monitor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Paired sensors transmitting real-time telemetry to clinical dispatch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onConnectDoctor,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("banner_call_doctor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Consult Doctor")
                    }
                }
            }
        }

        // 2. Real-Time Rolling ECG Waveform
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-Time Electrocardiogram (ECG)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FFC2))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active Wave Trace",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LiveEcgWaveform(
                    wavePoints = ecgPoints,
                    heartRate = liveVitals.heartRate
                )
            }
        }

        // 3. Scan action button with progress
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Take High-Accuracy Sensor Scan",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Synchronizes pulse oximeter, blood pressure cuff & thermometer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onScanNow,
                            enabled = !liveVitals.isScanning,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("scan_now_button")
                        ) {
                            Icon(
                                imageVector = if (liveVitals.isScanning) Icons.Default.Refresh else Icons.Default.Sensors,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (liveVitals.isScanning) "Scanning..." else "Scan Now")
                        }
                    }

                    if (liveVitals.isScanning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { liveVitals.scanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        // 4. Live Core Vitals Display (Heart Rate, Oxygen, Temperature)
        item {
            LiveVitalsDisplay(
                heartRate = liveVitals.heartRate,
                spo2 = liveVitals.spo2,
                temperatureF = liveVitals.temperatureF
            )
        }

        // 5. Extended Vital Parameters Detail
        item {
            Text(
                text = "Detailed Hemodynamic & Respiratory Metrics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            // Heart Rate & SpO2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val hrStatus = when {
                    liveVitals.heartRate > 120 -> MetricStatus.CRITICAL
                    liveVitals.heartRate > 100 || liveVitals.heartRate < 55 -> MetricStatus.WARNING
                    else -> MetricStatus.OPTIMAL
                }
                VitalMetricCard(
                    title = "Heart Rate",
                    value = liveVitals.heartRate.toString(),
                    unit = "BPM",
                    targetRange = "Resting: 60 - 100 bpm",
                    icon = Icons.Default.Favorite,
                    accentColor = VitalHeartRate,
                    status = hrStatus,
                    modifier = Modifier.weight(1f),
                    testTag = "vital_card_heart_rate"
                )

                val o2Status = when {
                    liveVitals.spo2 < 92 -> MetricStatus.CRITICAL
                    liveVitals.spo2 < 95 -> MetricStatus.WARNING
                    else -> MetricStatus.OPTIMAL
                }
                VitalMetricCard(
                    title = "SpO2 Oxygen",
                    value = "${liveVitals.spo2}%",
                    unit = "Sat",
                    targetRange = "Normal: 95% - 100%",
                    icon = Icons.Default.Air,
                    accentColor = VitalOxygen,
                    status = o2Status,
                    modifier = Modifier.weight(1f),
                    testTag = "vital_card_spo2"
                )
            }
        }

        item {
            // Blood Pressure & Temperature
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val bpStatus = when {
                    liveVitals.systolicBp >= 150 -> MetricStatus.CRITICAL
                    liveVitals.systolicBp >= 135 -> MetricStatus.WARNING
                    else -> MetricStatus.NORMAL
                }
                VitalMetricCard(
                    title = "Blood Pressure",
                    value = "${liveVitals.systolicBp}/${liveVitals.diastolicBp}",
                    unit = "mmHg",
                    targetRange = "Optimal: <120/80 mmHg",
                    icon = Icons.Default.Bloodtype,
                    accentColor = VitalBloodPressure,
                    status = bpStatus,
                    modifier = Modifier.weight(1f),
                    testTag = "vital_card_bp"
                )

                val tempStatus = when {
                    liveVitals.temperatureF >= 102.0f -> MetricStatus.CRITICAL
                    liveVitals.temperatureF >= 100.4f -> MetricStatus.WARNING
                    else -> MetricStatus.NORMAL
                }
                VitalMetricCard(
                    title = "Body Temp",
                    value = "${liveVitals.temperatureF}°",
                    unit = "F",
                    targetRange = "Normal: 97.8° - 99.1°F",
                    icon = Icons.Default.Thermostat,
                    accentColor = VitalTemperature,
                    status = tempStatus,
                    modifier = Modifier.weight(1f),
                    testTag = "vital_card_temp"
                )
            }
        }

        // 5. Simulation Testing Profiles
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Vitals Telemetry Simulation Modes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Test how the tablet and triage system respond to different clinical states:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimulationProfile.entries.forEach { profile ->
                            val isSelected = liveVitals.simulationMode == profile
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSimulationProfileChange(profile) },
                                label = { Text(profile.label, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // 6. Paired Medical Peripherals status
        item {
            Text(
                text = "Paired Health Hardware (${liveVitals.sensors.count { it.isConnected }}/${liveVitals.sensors.size} Online)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    liveVitals.sensors.forEachIndexed { index, sensor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (sensor.isConnected) TriageSafeGreen else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = sensor.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Sync: ${sensor.lastSyncTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BatteryFull,
                                    contentDescription = null,
                                    tint = TriageSafeGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${sensor.batteryPercent}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (index < liveVitals.sensors.size - 1) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    }
                }
            }
        }

        // 7. Recent Vitals Log
        if (vitalsHistory.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Recorded Readings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(vitalsHistory.take(5)) { record ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val formatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                            Text(
                                text = formatter.format(Date(record.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "HR: ${record.heartRate} bpm  |  SpO2: ${record.spo2}%  |  BP: ${record.systolicBp}/${record.diastolicBp}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (record.status == "Normal") TriageSafeGreen.copy(alpha = 0.12f)
                                    else TriageUrgentOrange.copy(alpha = 0.12f)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = record.status,
                                color = if (record.status == "Normal") TriageSafeGreen else TriageUrgentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
