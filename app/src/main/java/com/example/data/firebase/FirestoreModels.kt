package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreVitalsRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val heartRate: Int = 72,
    val spo2: Int = 98,
    val systolicBp: Int = 120,
    val diastolicBp: Int = 80,
    val temperatureF: Double = 98.6,
    val respirationRate: Int = 16,
    val status: String = "Normal",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreSymptomReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bodyRegion: String = "",
    val primarySymptom: String = "",
    val severity: Int = 1,
    val durationHours: Int = 1,
    val triageLevel: String = "SELF_CARE",
    val recommendation: String = "",
    val differentialAnalysis: String = "",
    val vitalsSnapshot: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreDoctorConsultation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val doctorName: String = "",
    val doctorSpecialty: String = "",
    val chiefComplaint: String = "",
    val clinicalSummary: String = "",
    val prescriptionSummary: String = "",
    val followUpAdvice: String = "",
    val callDurationMinutes: Int = 10,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreMedicationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val dosage: String = "",
    val timing: String = "",
    val isTaken: Boolean = false,
    val instructions: String = "",
    val prescribedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "",
    val message: String = "",
    val isFromUser: Boolean = true,
    val modelUsed: String = "gemini-3.5-flash",
    @ServerTimestamp val createdAt: Timestamp? = null
)
