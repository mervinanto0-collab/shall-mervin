package com.example.engine

import com.example.data.model.SensorDevice
import com.example.data.model.VitalsRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

data class LiveVitalsState(
    val heartRate: Int = 72,
    val spo2: Int = 98,
    val systolicBp: Int = 120,
    val diastolicBp: Int = 80,
    val temperatureF: Float = 98.6f,
    val respirationRate: Int = 16,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val status: String = "Normal",
    val simulationMode: SimulationProfile = SimulationProfile.NORMAL,
    val sensors: List<SensorDevice> = emptyList(),
    val isAllSensorsOnline: Boolean = true
)

enum class SimulationProfile(val label: String, val description: String) {
    NORMAL("Normal Resting", "Healthy resting vitals baseline"),
    ELEVATED_HEART_RATE("Tachycardia / Stress", "Heart rate elevated >100 bpm"),
    FEVER_ILLNESS("Fever & Chills", "Body temp 101.4°F with elevated respiration"),
    HYPERTENSIVE("Elevated Blood Pressure", "BP 145/95 mmHg")
}

class VitalsMonitorEngine(private val scope: CoroutineScope) {

    private val _vitalsState = MutableStateFlow(
        LiveVitalsState(
            sensors = listOf(
                SensorDevice("1", "CarePulse Oximeter Pro", "PULSE_OX", true, 92, 3, "Just now"),
                SensorDevice("2", "CarePulse Smart BP Cuff", "BP_CUFF", true, 88, 3, "1 min ago"),
                SensorDevice("3", "Infrared Core Thermometer", "THERMO", true, 95, 3, "Just now"),
                SensorDevice("4", "Continuous ECG Chest Lead", "ECG_PATCH", true, 76, 2, "Live Stream")
            )
        )
    )
    val vitalsState: StateFlow<LiveVitalsState> = _vitalsState.asStateFlow()

    // Real-time rolling ECG waveform points (0f..1f normalized)
    private val _ecgWave = MutableStateFlow<List<Float>>(List(120) { 0.5f })
    val ecgWave: StateFlow<List<Float>> = _ecgWave.asStateFlow()

    private var telemetryJob: Job? = null
    private var waveformJob: Job? = null
    private var phase: Float = 0f

    init {
        startTelemetryLoop()
        startWaveformLoop()
    }

    fun setSimulationProfile(profile: SimulationProfile) {
        _vitalsState.update { current ->
            when (profile) {
                SimulationProfile.NORMAL -> current.copy(
                    heartRate = 72,
                    spo2 = 98,
                    systolicBp = 120,
                    diastolicBp = 80,
                    temperatureF = 98.6f,
                    respirationRate = 16,
                    status = "Normal",
                    simulationMode = profile
                )
                SimulationProfile.ELEVATED_HEART_RATE -> current.copy(
                    heartRate = 108,
                    spo2 = 97,
                    systolicBp = 132,
                    diastolicBp = 86,
                    temperatureF = 99.1f,
                    respirationRate = 22,
                    status = "Elevated HR",
                    simulationMode = profile
                )
                SimulationProfile.FEVER_ILLNESS -> current.copy(
                    heartRate = 96,
                    spo2 = 96,
                    systolicBp = 124,
                    diastolicBp = 82,
                    temperatureF = 101.8f,
                    respirationRate = 20,
                    status = "Fever Detected",
                    simulationMode = profile
                )
                SimulationProfile.HYPERTENSIVE -> current.copy(
                    heartRate = 78,
                    spo2 = 98,
                    systolicBp = 148,
                    diastolicBp = 96,
                    temperatureF = 98.6f,
                    respirationRate = 16,
                    status = "Hypertensive Warning",
                    simulationMode = profile
                )
            }
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            while (isActive) {
                delay(2000)
                if (!_vitalsState.value.isScanning) {
                    val profile = _vitalsState.value.simulationMode
                    _vitalsState.update { current ->
                        val hrJitter = Random.nextInt(-2, 3)
                        val targetHr = when (profile) {
                            SimulationProfile.NORMAL -> 72
                            SimulationProfile.ELEVATED_HEART_RATE -> 106
                            SimulationProfile.FEVER_ILLNESS -> 95
                            SimulationProfile.HYPERTENSIVE -> 79
                        }
                        val newHr = (targetHr + hrJitter).coerceIn(45, 180)

                        val spo2Jitter = Random.nextInt(-1, 2)
                        val targetSpo2 = if (profile == SimulationProfile.FEVER_ILLNESS) 96 else 98
                        val newSpo2 = (targetSpo2 + spo2Jitter).coerceIn(88, 100)

                        val tempTarget = if (profile == SimulationProfile.FEVER_ILLNESS) 101.6f else 98.6f
                        val newTemp = (tempTarget + (Random.nextFloat() * 0.4f - 0.2f)).coerceIn(95.0f, 105.0f)

                        current.copy(
                            heartRate = newHr,
                            spo2 = newSpo2,
                            temperatureF = ((newTemp * 10).toInt() / 10f)
                        )
                    }
                }
            }
        }
    }

    private fun startWaveformLoop() {
        waveformJob?.cancel()
        waveformJob = scope.launch {
            while (isActive) {
                delay(33) // ~30 fps smooth trace update
                phase += 0.08f
                if (phase > 2f * PI) phase = 0f

                // Generate realistic P-QRS-T electrocardiogram wave calculation
                val hrFactor = _vitalsState.value.heartRate / 72f
                val t = (phase * hrFactor) % (2f * PI.toFloat())

                // Baseline is 0.5f (middle)
                var y = 0.5f

                // P wave: small bump
                if (t in 0.5f..1.1f) {
                    y += 0.08f * sin((t - 0.5f) / 0.6f * PI.toFloat())
                }
                // Q wave: small dip
                else if (t in 1.3f..1.45f) {
                    y -= 0.06f * sin((t - 1.3f) / 0.15f * PI.toFloat())
                }
                // R wave: sharp upward spike
                else if (t in 1.45f..1.7f) {
                    val progress = (t - 1.45f) / 0.25f
                    y += 0.44f * sin(progress * PI.toFloat())
                }
                // S wave: sharp downward dip
                else if (t in 1.7f..1.9f) {
                    val progress = (t - 1.7f) / 0.2f
                    y -= 0.16f * sin(progress * PI.toFloat())
                }
                // T wave: gentle rounded wave
                else if (t in 2.3f..3.1f) {
                    y += 0.14f * sin((t - 2.3f) / 0.8f * PI.toFloat())
                }

                // Add slight analog sensor noise
                y += (Random.nextFloat() - 0.5f) * 0.015f
                val clampedY = y.coerceIn(0.05f, 0.95f)

                _ecgWave.update { points ->
                    val mutable = points.toMutableList()
                    if (mutable.size >= 120) {
                        mutable.removeAt(0)
                    }
                    mutable.add(clampedY)
                    mutable
                }
            }
        }
    }

    suspend fun performManualVitalsScan(onComplete: (VitalsRecord) -> Unit) {
        _vitalsState.update { it.copy(isScanning = true, scanProgress = 0f) }
        val steps = 20
        for (i in 1..steps) {
            delay(120)
            _vitalsState.update { it.copy(scanProgress = i / steps.toFloat()) }
        }

        val current = _vitalsState.value
        val record = VitalsRecord(
            heartRate = current.heartRate,
            spo2 = current.spo2,
            systolicBp = current.systolicBp,
            diastolicBp = current.diastolicBp,
            temperatureF = current.temperatureF,
            respirationRate = current.respirationRate,
            status = if (current.heartRate > 100 || current.temperatureF > 100.4f || current.systolicBp > 140) "Alert" else "Normal",
            notes = "Synchronized scan via CarePulse Tablet Station sensors"
        )

        _vitalsState.update { it.copy(isScanning = false, scanProgress = 1f) }
        onComplete(record)
    }

    fun toggleSensorConnection(sensorId: String) {
        _vitalsState.update { current ->
            val updatedSensors = current.sensors.map { sensor ->
                if (sensor.id == sensorId) {
                    sensor.copy(isConnected = !sensor.isConnected)
                } else sensor
            }
            current.copy(
                sensors = updatedSensors,
                isAllSensorsOnline = updatedSensors.all { it.isConnected }
            )
        }
    }
}
