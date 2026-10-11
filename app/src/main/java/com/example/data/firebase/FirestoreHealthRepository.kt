package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreHealthRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Vitals ---
    fun observeVitals(): Flow<List<FirestoreVitalsRecord>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "users/$uid/vitals"
        val query = db.collection("users").document(uid).collection("vitals")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val records = snapshot.toObjects(FirestoreVitalsRecord::class.java)
                trySend(records)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun saveVitals(
        heartRate: Int,
        spo2: Int,
        systolicBp: Int,
        diastolicBp: Int,
        temperatureF: Float,
        respirationRate: Int,
        status: String,
        notes: String
    ): String {
        val uid = requireUserId()
        val path = "users/$uid/vitals"
        val docRef = db.collection("users").document(uid).collection("vitals").document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "heartRate" to heartRate,
            "spo2" to spo2,
            "systolicBp" to systolicBp,
            "diastolicBp" to diastolicBp,
            "temperatureF" to temperatureF.toDouble(),
            "respirationRate" to respirationRate,
            "status" to status,
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    // --- Symptom Reports ---
    fun observeSymptoms(): Flow<List<FirestoreSymptomReport>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "users/$uid/symptoms"
        val query = db.collection("users").document(uid).collection("symptoms")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val reports = snapshot.toObjects(FirestoreSymptomReport::class.java)
                trySend(reports)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun saveSymptomReport(
        bodyRegion: String,
        primarySymptom: String,
        severity: Int,
        durationHours: Int,
        triageLevel: String,
        recommendation: String,
        differentialAnalysis: String,
        vitalsSnapshot: String,
        notes: String
    ): String {
        val uid = requireUserId()
        val path = "users/$uid/symptoms"
        val docRef = db.collection("users").document(uid).collection("symptoms").document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "bodyRegion" to bodyRegion,
            "primarySymptom" to primarySymptom,
            "severity" to severity,
            "durationHours" to durationHours,
            "triageLevel" to triageLevel,
            "recommendation" to recommendation,
            "differentialAnalysis" to differentialAnalysis,
            "vitalsSnapshot" to vitalsSnapshot,
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    // --- Consultations ---
    fun observeConsultations(): Flow<List<FirestoreDoctorConsultation>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "users/$uid/consultations"
        val query = db.collection("users").document(uid).collection("consultations")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(FirestoreDoctorConsultation::class.java)
                trySend(items)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun saveConsultation(
        doctorName: String,
        doctorSpecialty: String,
        chiefComplaint: String,
        clinicalSummary: String,
        prescriptionSummary: String,
        followUpAdvice: String,
        callDurationMinutes: Int
    ): String {
        val uid = requireUserId()
        val path = "users/$uid/consultations"
        val docRef = db.collection("users").document(uid).collection("consultations").document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "doctorName" to doctorName,
            "doctorSpecialty" to doctorSpecialty,
            "chiefComplaint" to chiefComplaint,
            "clinicalSummary" to clinicalSummary,
            "prescriptionSummary" to prescriptionSummary,
            "followUpAdvice" to followUpAdvice,
            "callDurationMinutes" to callDurationMinutes,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    // --- Medications ---
    fun observeMedications(): Flow<List<FirestoreMedicationItem>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "users/$uid/medications"
        val query = db.collection("users").document(uid).collection("medications")

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(FirestoreMedicationItem::class.java)
                trySend(items)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun setMedicationTaken(medicationId: String, isTaken: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("medications").document(medicationId)
        try {
            docRef.update(
                mapOf(
                    "isTaken" to isTaken,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Chat Messages (Multi-Turn Gemini History) ---
    fun observeChatMessages(): Flow<List<FirestoreChatMessage>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val path = "users/$uid/chat_messages"
        val query = db.collection("users").document(uid).collection("chat_messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(FirestoreChatMessage::class.java)
                trySend(items)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun saveChatMessage(
        sender: String,
        message: String,
        isFromUser: Boolean,
        modelUsed: String
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chat_messages").document()
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "sender" to sender,
            "message" to message,
            "isFromUser" to isFromUser,
            "modelUsed" to modelUsed,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun seedInitialCloudDataIfEmpty(uid: String) {
        val medsCol = db.collection("users").document(uid).collection("medications")
        val existing = medsCol.limit(1).get().await()
        if (existing.isEmpty) {
            val defaultMeds = listOf(
                hashMapOf(
                    "userId" to uid,
                    "name" to "Lisinopril",
                    "dosage" to "10mg Tablet",
                    "timing" to "Morning 8:00 AM",
                    "isTaken" to true,
                    "instructions" to "Take once daily for blood pressure management",
                    "prescribedBy" to "Dr. Sarah Chen",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                hashMapOf(
                    "userId" to uid,
                    "name" to "Metformin HCl",
                    "dosage" to "500mg Tablet",
                    "timing" to "Morning with meal",
                    "isTaken" to true,
                    "instructions" to "Take with breakfast",
                    "prescribedBy" to "Dr. Marcus Vance",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                hashMapOf(
                    "userId" to uid,
                    "name" to "Atorvastatin",
                    "dosage" to "20mg Tablet",
                    "timing" to "Evening 8:00 PM",
                    "isTaken" to false,
                    "instructions" to "Take at bedtime with water",
                    "prescribedBy" to "Dr. Marcus Vance",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                hashMapOf(
                    "userId" to uid,
                    "name" to "Vitamin D3 + K2",
                    "dosage" to "2000 IU Capsule",
                    "timing" to "Morning 8:00 AM",
                    "isTaken" to false,
                    "instructions" to "Take with water",
                    "prescribedBy" to "Dr. Sarah Chen",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            for (m in defaultMeds) {
                medsCol.add(m).await()
            }

            // Also seed welcome chat message
            val chatCol = db.collection("users").document(uid).collection("chat_messages")
            chatCol.add(
                hashMapOf(
                    "userId" to uid,
                    "sender" to "Dr. CarePulse AI",
                    "message" to "Hello! I am Dr. CarePulse AI, your clinical tele-triage assistant. I can analyze your symptoms in real-time, explain medical questions, review medication instructions, and monitor your vitals. How are you feeling today?",
                    "isFromUser" to false,
                    "modelUsed" to "gemini-3.5-flash",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }
}
