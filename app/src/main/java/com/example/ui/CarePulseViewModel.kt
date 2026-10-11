package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.AiClinicalRole
import com.example.ai.ChatTurn
import com.example.ai.GeminiService
import com.example.data.firebase.FirestoreChatMessage
import com.example.data.firebase.FirestoreDoctorConsultation
import com.example.data.firebase.FirestoreHealthRepository
import com.example.data.firebase.FirestoreMedicationItem
import com.example.data.firebase.FirestoreSymptomReport
import com.example.data.firebase.FirestoreVitalsRecord
import com.example.data.local.CarePulseDatabase
import com.example.data.model.Doctor
import com.example.data.model.DoctorConsultation
import com.example.data.model.MedicationItem
import com.example.data.model.SensorDevice
import com.example.data.model.SymptomReport
import com.example.data.model.TriageLevel
import com.example.data.model.TriageResult
import com.example.data.model.VitalsRecord
import com.example.data.repository.HealthRepository
import com.example.engine.LiveVitalsState
import com.example.engine.SimulationProfile
import com.example.engine.SymptomAnalysisEngine
import com.example.engine.VitalsMonitorEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ScreenDestination {
    HEALTH_MONITOR,
    SYMPTOM_ANALYSIS,
    AI_ASSISTANT,
    TELECONSULTATION,
    CARE_PLAN,
    PATIENT_PROFILE
}

data class VideoCallState(
    val isActive: Boolean = false,
    val isConnecting: Boolean = false,
    val activeDoctor: Doctor? = null,
    val callDurationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val doctorMessage: String = "",
    val inCallChatMessages: List<ChatMessage> = emptyList(),
    val isConsultationCompleted: Boolean = false,
    val lastGeneratedSummary: DoctorConsultation? = null
)

data class ChatMessage(
    val sender: String,
    val message: String,
    val timestamp: String,
    val isFromDoctor: Boolean
)

data class SymptomInputState(
    val selectedRegion: String = "Chest & Heart",
    val selectedSymptom: String = "Chest tightness / Pressure",
    val severity: Int = 4,
    val durationHours: Int = 6,
    val additionalNotes: String = "",
    val triageResult: TriageResult? = null
)

class CarePulseViewModel(application: Application) : AndroidViewModel(application) {

    private val localDatabase = CarePulseDatabase.getDatabase(application)
    private val localRepository = HealthRepository(localDatabase.dao())
    private val vitalsEngine = VitalsMonitorEngine(viewModelScope)
    private val cloudRepository = FirestoreHealthRepository(application)
    private val geminiService = GeminiService()

    // Firebase Auth session state
    private val _currentUser = MutableStateFlow<FirebaseUser?>(FirebaseAuth.getInstance().currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUser.value = auth.currentUser
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                cloudRepository.seedInitialCloudDataIfEmpty(user.uid)
            }
        }
    }

    // Current navigation tab
    private val _currentScreen = MutableStateFlow(ScreenDestination.HEALTH_MONITOR)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    // Emergency SOS dialog visibility
    private val _showSosDialog = MutableStateFlow(false)
    val showSosDialog: StateFlow<Boolean> = _showSosDialog.asStateFlow()

    // Telemetry state & waveform
    val liveVitals: StateFlow<LiveVitalsState> = vitalsEngine.vitalsState
    val ecgWavePoints: StateFlow<List<Float>> = vitalsEngine.ecgWave

    // Database reactive streams (Local + Cloud sync)
    val allVitals: StateFlow<List<VitalsRecord>> = localRepository.allVitals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSymptomReports: StateFlow<List<SymptomReport>> = localRepository.allSymptomReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConsultations: StateFlow<List<DoctorConsultation>> = localRepository.allConsultations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMedications: StateFlow<List<MedicationItem>> = localRepository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cloud Chat Messages for Gemini Assistant
    val cloudChatMessages: StateFlow<List<FirestoreChatMessage>> = cloudRepository.observeChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Clinical State
    private val _selectedAiRole = MutableStateFlow(AiClinicalRole.GENERAL_DOCTOR)
    val selectedAiRole: StateFlow<AiClinicalRole> = _selectedAiRole.asStateFlow()

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    // Fallback local memory for AI chat if unauthenticated
    private val _localAiMessages = MutableStateFlow<List<FirestoreChatMessage>>(
        listOf(
            FirestoreChatMessage(
                sender = "Dr. CarePulse AI",
                message = "Hello! I am Dr. CarePulse AI, your clinical tele-triage assistant. I can analyze your symptoms in real-time, explain medical questions, review medication instructions, and monitor your vitals. How are you feeling today?",
                isFromUser = false,
                modelUsed = "gemini-3.5-flash"
            )
        )
    )

    // Merged reactive chat message stream (prefers cloud if logged in, else local)
    val displayChatMessages: StateFlow<List<FirestoreChatMessage>> = combine(
        currentUser,
        cloudChatMessages,
        _localAiMessages
    ) { user, cloudMsgs, localMsgs ->
        if (user != null && cloudMsgs.isNotEmpty()) cloudMsgs else localMsgs
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Symptom checker input state
    private val _symptomInput = MutableStateFlow(SymptomInputState())
    val symptomInput: StateFlow<SymptomInputState> = _symptomInput.asStateFlow()

    // Video Consultation state
    private val _videoCallState = MutableStateFlow(VideoCallState())
    val videoCallState: StateFlow<VideoCallState> = _videoCallState.asStateFlow()
    private var callTimerJob: Job? = null

    // Available Remote Doctors
    val availableDoctors = listOf(
        Doctor(
            id = "doc_1",
            name = "Dr. Sarah Chen, MD",
            title = "Internal Medicine & Tele-Triage Specialist",
            specialty = "Primary Care & Acute Triage",
            hospital = "Metropolitan Telehealth Center",
            rating = 4.95f,
            reviewsCount = 428,
            isAvailableNow = true,
            avatarDrawable = R.drawable.img_doctor_sarah,
            waitTimeMinutes = 2
        ),
        Doctor(
            id = "doc_2",
            name = "Dr. Marcus Vance, MD",
            title = "Cardiologist & Vascular Specialist",
            specialty = "Cardiology & Telemetry Review",
            hospital = "Cardiac Health Institute",
            rating = 4.98f,
            reviewsCount = 612,
            isAvailableNow = true,
            avatarDrawable = R.drawable.img_doctor_marcus,
            waitTimeMinutes = 5
        )
    )

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
        viewModelScope.launch {
            localRepository.seedInitialDataIfEmpty()
            recalculateTriage()
        }
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun openSosDialog() {
        _showSosDialog.value = true
    }

    fun dismissSosDialog() {
        _showSosDialog.value = false
    }

    fun setSimulationProfile(profile: SimulationProfile) {
        vitalsEngine.setSimulationProfile(profile)
        recalculateTriage()
    }

    fun toggleSensorConnection(sensorId: String) {
        vitalsEngine.toggleSensorConnection(sensorId)
    }

    fun performVitalsScan() {
        viewModelScope.launch {
            vitalsEngine.performManualVitalsScan { record ->
                viewModelScope.launch {
                    localRepository.recordVitals(record)

                    // Cloud Firestore sync
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        try {
                            cloudRepository.saveVitals(
                                heartRate = record.heartRate,
                                spo2 = record.spo2,
                                systolicBp = record.systolicBp,
                                diastolicBp = record.diastolicBp,
                                temperatureF = record.temperatureF,
                                respirationRate = record.respirationRate,
                                status = record.status,
                                notes = record.notes
                            )
                        } catch (e: Exception) {
                            // Handled & logged by cloudRepository
                        }
                    }

                    recalculateTriage()
                }
            }
        }
    }

    // Symptom Analysis interactions
    fun updateSelectedRegion(region: String) {
        val symptoms = SymptomAnalysisEngine.symptomsByRegion[region] ?: emptyList()
        val defaultSymptom = symptoms.firstOrNull() ?: ""
        _symptomInput.update {
            it.copy(selectedRegion = region, selectedSymptom = defaultSymptom)
        }
        recalculateTriage()
    }

    fun updateSelectedSymptom(symptom: String) {
        _symptomInput.update { it.copy(selectedSymptom = symptom) }
        recalculateTriage()
    }

    fun updateSeverity(severity: Int) {
        _symptomInput.update { it.copy(severity = severity) }
        recalculateTriage()
    }

    fun updateDuration(hours: Int) {
        _symptomInput.update { it.copy(durationHours = hours) }
        recalculateTriage()
    }

    fun updateAdditionalNotes(notes: String) {
        _symptomInput.update { it.copy(additionalNotes = notes) }
    }

    fun recalculateTriage() {
        val input = _symptomInput.value
        val vitals = liveVitals.value
        val result = SymptomAnalysisEngine.analyzeSymptoms(
            region = input.selectedRegion,
            symptom = input.selectedSymptom,
            severity = input.severity,
            durationHours = input.durationHours,
            heartRate = vitals.heartRate,
            spo2 = vitals.spo2,
            systolicBp = vitals.systolicBp,
            temperatureF = vitals.temperatureF
        )
        _symptomInput.update { it.copy(triageResult = result) }
    }

    fun saveCurrentSymptomReport() {
        viewModelScope.launch {
            val input = _symptomInput.value
            val vitals = liveVitals.value
            val result = input.triageResult ?: return@launch

            val snapshot = "HR: ${vitals.heartRate} bpm | SpO2: ${vitals.spo2}% | BP: ${vitals.systolicBp}/${vitals.diastolicBp} | Temp: ${vitals.temperatureF}°F"

            val report = SymptomReport(
                bodyRegion = input.selectedRegion,
                primarySymptom = input.selectedSymptom,
                severity = input.severity,
                durationHours = input.durationHours,
                notes = input.additionalNotes,
                triageLevel = result.level.name,
                recommendation = result.primaryRecommendation,
                differentialAnalysis = result.possibleConditions.joinToString(", "),
                vitalsSnapshot = snapshot
            )
            localRepository.saveSymptomReport(report)

            // Cloud sync
            if (FirebaseAuth.getInstance().currentUser != null) {
                try {
                    cloudRepository.saveSymptomReport(
                        bodyRegion = input.selectedRegion,
                        primarySymptom = input.selectedSymptom,
                        severity = input.severity,
                        durationHours = input.durationHours,
                        triageLevel = result.level.name,
                        recommendation = result.primaryRecommendation,
                        differentialAnalysis = result.possibleConditions.joinToString(", "),
                        vitalsSnapshot = snapshot,
                        notes = input.additionalNotes
                    )
                } catch (e: Exception) {
                    // Handled
                }
            }
        }
    }

    // Gemini AI Multi-turn Chat
    fun selectAiRole(role: AiClinicalRole) {
        _selectedAiRole.value = role
    }

    fun sendAiUserMessage(prompt: String, attachVitals: Boolean) {
        if (prompt.isBlank() || _isAiGenerating.value) return

        val role = _selectedAiRole.value
        val vitals = liveVitals.value
        val vitalsSnapshot = if (attachVitals) {
            "Heart Rate: ${vitals.heartRate} BPM | SpO2: ${vitals.spo2}% | Blood Pressure: ${vitals.systolicBp}/${vitals.diastolicBp} mmHg | Temperature: ${vitals.temperatureF}°F"
        } else null

        // 1. Add user message
        val userMsg = FirestoreChatMessage(
            sender = "Patient",
            message = prompt,
            isFromUser = true,
            modelUsed = role.modelId
        )

        val updatedLocal = _localAiMessages.value.toMutableList().apply { add(userMsg) }
        _localAiMessages.value = updatedLocal

        if (FirebaseAuth.getInstance().currentUser != null) {
            viewModelScope.launch {
                try {
                    cloudRepository.saveChatMessage(
                        sender = "Patient",
                        message = prompt,
                        isFromUser = true,
                        modelUsed = role.modelId
                    )
                } catch (e: Exception) {
                    // Handled
                }
            }
        }

        // 2. Query Gemini in background
        _isAiGenerating.value = true
        viewModelScope.launch {
            val history = displayChatMessages.value.map {
                ChatTurn(role = if (it.isFromUser) "user" else "model", text = it.message)
            }

            val response = geminiService.generateChatResponse(
                role = role,
                history = history,
                latestUserMessage = prompt,
                patientVitalsContext = vitalsSnapshot
            )

            val aiMsg = FirestoreChatMessage(
                sender = role.title,
                message = response,
                isFromUser = false,
                modelUsed = role.modelId
            )

            val nextLocal = _localAiMessages.value.toMutableList().apply { add(aiMsg) }
            _localAiMessages.value = nextLocal
            _isAiGenerating.value = false

            if (FirebaseAuth.getInstance().currentUser != null) {
                try {
                    cloudRepository.saveChatMessage(
                        sender = role.title,
                        message = response,
                        isFromUser = false,
                        modelUsed = role.modelId
                    )
                } catch (e: Exception) {
                    // Handled
                }
            }
        }
    }

    // Teleconsultation lifecycle
    fun startConsultation(doctor: Doctor) {
        _videoCallState.value = VideoCallState(
            isActive = true,
            isConnecting = true,
            activeDoctor = doctor,
            doctorMessage = "Connecting secure encrypted telemedicine channel..."
        )
        _currentScreen.value = ScreenDestination.TELECONSULTATION

        viewModelScope.launch {
            delay(1500)
            _videoCallState.update {
                it.copy(
                    isConnecting = false,
                    doctorMessage = "Hello! I'm ${doctor.name}. I'm reviewing your real-time tablet telemetry right now.",
                    inCallChatMessages = listOf(
                        ChatMessage(
                            sender = doctor.name,
                            message = "Connection established. I can see your live vitals: HR ${liveVitals.value.heartRate} bpm, SpO2 ${liveVitals.value.spo2}%. How are you feeling right now?",
                            timestamp = "Just now",
                            isFromDoctor = true
                        )
                    )
                )
            }
            startCallTimer()
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (isActive && _videoCallState.value.isActive) {
                delay(1000)
                _videoCallState.update { it.copy(callDurationSeconds = it.callDurationSeconds + 1) }

                val secs = _videoCallState.value.callDurationSeconds
                if (secs == 12) {
                    addDoctorChatMessage("Your oxygen saturation and blood pressure look stable. Let's make sure you take your prescribed regimen.")
                } else if (secs == 25) {
                    addDoctorChatMessage("I am sending an updated prescription and clinical summary to your Care Plan tab.")
                }
            }
        }
    }

    fun sendPatientMessage(text: String) {
        if (text.isBlank()) return
        val currentMsgs = _videoCallState.value.inCallChatMessages.toMutableList()
        currentMsgs.add(
            ChatMessage(
                sender = "You (Patient)",
                message = text,
                timestamp = "Just now",
                isFromDoctor = false
            )
        )
        _videoCallState.update { it.copy(inCallChatMessages = currentMsgs) }

        viewModelScope.launch {
            delay(1800)
            val doctor = _videoCallState.value.activeDoctor ?: return@launch
            val response = when {
                text.contains("pain", ignoreCase = true) || text.contains("chest", ignoreCase = true) ->
                    "Understood. Please keep resting in an upright position. The monitor shows no acute ischemic ST changes on Lead II."
                text.contains("medication", ignoreCase = true) || text.contains("pill", ignoreCase = true) ->
                    "Yes, continue taking your morning dose as scheduled with a full glass of water."
                else ->
                    "Thank you for sharing that. I am documenting this in your medical record."
            }
            addDoctorChatMessage(response)
        }
    }

    private fun addDoctorChatMessage(msg: String) {
        val doctor = _videoCallState.value.activeDoctor ?: return
        val currentMsgs = _videoCallState.value.inCallChatMessages.toMutableList()
        currentMsgs.add(
            ChatMessage(
                sender = doctor.name,
                message = msg,
                timestamp = "Just now",
                isFromDoctor = true
            )
        )
        _videoCallState.update {
            it.copy(doctorMessage = msg, inCallChatMessages = currentMsgs)
        }
    }

    fun toggleMute() {
        _videoCallState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleCamera() {
        _videoCallState.update { it.copy(isCameraOff = !it.isCameraOff) }
    }

    fun endConsultation() {
        val doctor = _videoCallState.value.activeDoctor ?: return
        val duration = (_videoCallState.value.callDurationSeconds / 60).coerceAtLeast(1)
        val vitals = liveVitals.value

        val summary = DoctorConsultation(
            doctorName = doctor.name,
            doctorSpecialty = doctor.specialty,
            chiefComplaint = _symptomInput.value.selectedSymptom,
            clinicalSummary = "Tele-evaluated patient via CarePulse tablet station. Live telemetry confirmed HR ${vitals.heartRate} bpm, SpO2 ${vitals.spo2}%, BP ${vitals.systolicBp}/${vitals.diastolicBp} mmHg. Patient reported ${_symptomInput.value.selectedSymptom} with severity ${_symptomInput.value.severity}/10.",
            prescriptionSummary = "Prescribed digital Care Plan updates. Advised continuing Lisinopril 10mg daily and regular hydration.",
            followUpAdvice = "Monitor vitals on tablet morning and evening. Re-consult if symptoms escalate.",
            callDurationMinutes = duration
        )

        viewModelScope.launch {
            localRepository.saveConsultation(summary)

            // Cloud Firestore sync
            if (FirebaseAuth.getInstance().currentUser != null) {
                try {
                    cloudRepository.saveConsultation(
                        doctorName = doctor.name,
                        doctorSpecialty = doctor.specialty,
                        chiefComplaint = _symptomInput.value.selectedSymptom,
                        clinicalSummary = summary.clinicalSummary,
                        prescriptionSummary = summary.prescriptionSummary,
                        followUpAdvice = summary.followUpAdvice,
                        callDurationMinutes = duration
                    )
                } catch (e: Exception) {
                    // Handled
                }
            }
        }

        callTimerJob?.cancel()
        _videoCallState.update {
            it.copy(
                isActive = false,
                isConsultationCompleted = true,
                lastGeneratedSummary = summary
            )
        }
    }

    fun resetConsultationCompletedState() {
        _videoCallState.update { it.copy(isConsultationCompleted = false) }
    }

    // Care plan
    fun toggleMedicationTaken(medicationId: Long, currentTaken: Boolean) {
        viewModelScope.launch {
            localRepository.setMedicationTaken(medicationId, !currentTaken)
        }
    }
}
