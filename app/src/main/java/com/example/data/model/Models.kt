package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vitals_records")
data class VitalsRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val heartRate: Int,
    val spo2: Int,
    val systolicBp: Int,
    val diastolicBp: Int,
    val temperatureF: Float,
    val respirationRate: Int,
    val status: String = "Normal",
    val notes: String = ""
)

@Entity(tableName = "symptom_reports")
data class SymptomReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val bodyRegion: String,
    val primarySymptom: String,
    val severity: Int, // 1 to 10
    val durationHours: Int,
    val notes: String = "",
    val triageLevel: String, // EMERGENCY, URGENT, TELECONSULT, SELF_CARE
    val recommendation: String,
    val differentialAnalysis: String,
    val vitalsSnapshot: String
)

@Entity(tableName = "doctor_consultations")
data class DoctorConsultation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val doctorName: String,
    val doctorSpecialty: String,
    val chiefComplaint: String,
    val clinicalSummary: String,
    val prescriptionSummary: String,
    val followUpAdvice: String,
    val callDurationMinutes: Int = 12
)

@Entity(tableName = "medications")
data class MedicationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val timing: String, // e.g. "Morning 8:00 AM", "Evening 8:00 PM"
    val isTaken: Boolean = false,
    val instructions: String = "Take with water",
    val prescribedBy: String = "Dr. Sarah Chen"
)

data class Doctor(
    val id: String,
    val name: String,
    val title: String,
    val specialty: String,
    val hospital: String,
    val rating: Float,
    val reviewsCount: Int,
    val isAvailableNow: Boolean,
    val avatarDrawable: Int,
    val waitTimeMinutes: Int
)

data class SensorDevice(
    val id: String,
    val name: String,
    val type: String, // "PULSE_OX", "BP_CUFF", "THERMO", "ECG_PATCH"
    val isConnected: Boolean,
    val batteryPercent: Int,
    val signalStrength: Int, // 1..3
    val lastSyncTime: String
)

enum class TriageLevel {
    EMERGENCY,
    URGENT,
    TELECONSULT,
    SELF_CARE
}

data class TriageResult(
    val level: TriageLevel,
    val title: String,
    val urgencySubtitle: String,
    val primaryRecommendation: String,
    val possibleConditions: List<String>,
    val actionSteps: List<String>,
    val requiresImmediateDoctor: Boolean,
    val isEmergencyFlag: Boolean
)
