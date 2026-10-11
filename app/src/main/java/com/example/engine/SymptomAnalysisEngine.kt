package com.example.engine

import com.example.data.model.TriageLevel
import com.example.data.model.TriageResult

object SymptomAnalysisEngine {

    val bodyRegions = listOf(
        "Chest & Heart",
        "Head & Neurological",
        "Respiratory & Throat",
        "Abdomen & Digestive",
        "Musculoskeletal & Joints",
        "General / Systemic"
    )

    val symptomsByRegion = mapOf(
        "Chest & Heart" to listOf(
            "Chest tightness / Pressure",
            "Palpitations / Fluttering heartbeat",
            "Sharp pain with breathing",
            "Sudden racing pulse"
        ),
        "Head & Neurological" to listOf(
            "Severe throbbing headache",
            "Dizziness / Vertigo / Lightheadedness",
            "Sudden visual disturbance",
            "Tension in temples & neck"
        ),
        "Respiratory & Throat" to listOf(
            "Shortness of breath on exertion",
            "Persistent dry or productive cough",
            "Wheezing / Stridor",
            "Sore throat & swollen glands"
        ),
        "Abdomen & Digestive" to listOf(
            "Sharp upper abdominal pain",
            "Nausea and vomiting",
            "Persistent cramps / Bloating",
            "Heartburn / Acid reflux"
        ),
        "Musculoskeletal & Joints" to listOf(
            "Acute lower back strain",
            "Swollen knee / Ankle pain",
            "Shoulder impingement pain",
            "Neck stiffness & spasm"
        ),
        "General / Systemic" to listOf(
            "Fever with chills & body aches",
            "Sudden severe fatigue / Weakness",
            "Dehydration / Dry mouth",
            "Skin rash / Itching"
        )
    )

    fun analyzeSymptoms(
        region: String,
        symptom: String,
        severity: Int, // 1 to 10
        durationHours: Int,
        heartRate: Int,
        spo2: Int,
        systolicBp: Int,
        temperatureF: Float
    ): TriageResult {
        // 1. Red Flag Emergency Checks
        val isLowO2 = spo2 < 92
        val isSevereTachycardia = heartRate > 125
        val isHypertensiveCrisis = systolicBp >= 180
        val isHighFever = temperatureF >= 103.5f

        val isChestEmergency = region == "Chest & Heart" && (
                symptom.contains("tightness", ignoreCase = true) ||
                symptom.contains("Pressure", ignoreCase = true) ||
                severity >= 8 ||
                isLowO2 ||
                isSevereTachycardia
        )

        val isNeuroEmergency = region == "Head & Neurological" && (
                severity >= 9 || (symptom.contains("Sudden visual", ignoreCase = true) && systolicBp > 160)
        )

        val isRespEmergency = isLowO2 && (region == "Respiratory & Throat" || severity >= 7)

        if (isChestEmergency || isNeuroEmergency || isRespEmergency || isHypertensiveCrisis) {
            return TriageResult(
                level = TriageLevel.EMERGENCY,
                title = "Emergency Medical Alert",
                urgencySubtitle = "Immediate clinical intervention required",
                primaryRecommendation = "Critical red flag indicators detected based on symptoms and telemetry (SpO2: $spo2%, HR: $heartRate bpm, BP: ${systolicBp}mmHg). Call 911 or activate the emergency dispatch button.",
                possibleConditions = listOf(
                    "Acute Coronary Syndrome / Angina",
                    "Acute Hypoxemic Respiratory Distress",
                    "Hypertensive Crisis / Cerebrovascular Alert"
                ),
                actionSteps = listOf(
                    "Tap the Emergency SOS button below immediately",
                    "Rest in a comfortable seated position with head elevated",
                    "Do not drive yourself to hospital; wait for paramedics",
                    "Keep CarePulse tablet near you for automated paramedic telemetry transmission"
                ),
                requiresImmediateDoctor = true,
                isEmergencyFlag = true
            )
        }

        // 2. Urgent Condition (Needs urgent remote doctor within 15-30 minutes)
        if (severity >= 7 || temperatureF >= 101.5f || heartRate > 105 || systolicBp >= 145 || spo2 <= 94) {
            return TriageResult(
                level = TriageLevel.URGENT,
                title = "Urgent Teleconsultation Recommended",
                urgencySubtitle = "Physician evaluation advised within 30 minutes",
                primaryRecommendation = "Elevated physiological stress and moderate-to-high symptom distress observed. A video teleconsultation will review your vital trends and issue necessary prescriptions.",
                possibleConditions = listOf(
                    if (temperatureF > 100.4f) "Active viral/bacterial febrile illness" else "Symptomatic Cardiovascular Strain",
                    "Bronchial inflammation or reactive airway syndrome",
                    "Severe Migraine / Cluster Episode"
                ),
                actionSteps = listOf(
                    "Click 'Start Instant Doctor Consultation' to connect with an on-duty physician",
                    "Hydrate with small sips of water",
                    "Avoid strenuous activity or sudden posture changes",
                    "Leave health sensors connected for real-time doctor telemetry sharing"
                ),
                requiresImmediateDoctor = true,
                isEmergencyFlag = false
            )
        }

        // 3. Teleconsult Scheduled / Routine (Symptom distress 4 to 6)
        if (severity >= 4 || durationHours > 24) {
            return TriageResult(
                level = TriageLevel.TELECONSULT,
                title = "Remote Physician Consultation Recommended",
                urgencySubtitle = "Non-emergency telemedicine consultation",
                primaryRecommendation = "Your vital signs are stable (HR: $heartRate bpm, SpO2: $spo2%). However, symptoms have persisted for $durationHours hours with severity $severity/10. A remote consult is recommended for diagnostic clarity and digital prescription.",
                possibleConditions = listOf(
                    "Upper respiratory tract infection",
                    "Tension cephalalgia or cervicogenic discomfort",
                    "Functional gastrointestinal distress or gastritis",
                    "Musculoskeletal myofascial trigger irritation"
                ),
                actionSteps = listOf(
                    "Schedule or enter the queue for Dr. Sarah Chen or Dr. Marcus Vance",
                    "Review your current medications under the Care Plan tab",
                    "Log vitals again in 2 hours to monitor trend trajectory",
                    "Prepare notes on symptom triggers for the physician"
                ),
                requiresImmediateDoctor = false,
                isEmergencyFlag = false
            )
        }

        // 4. Self-Care / Monitored Rest
        return TriageResult(
            level = TriageLevel.SELF_CARE,
            title = "Self-Care & Continued Tele-Monitoring",
            urgencySubtitle = "Mild symptoms with normal physiological vitals",
            primaryRecommendation = "Your current vitals are within normal clinical thresholds (HR: $heartRate bpm, SpO2: $spo2%, Temp: ${temperatureF}°F). Symptom severity ($severity/10) is mild and suitable for home management with tablet monitoring.",
            possibleConditions = listOf(
                "Mild transient fatigue or dehydration",
                "Minor muscle tension / posture fatigue",
                "Benign early viral prodrome"
            ),
            actionSteps = listOf(
                "Rest in a quiet, well-ventilated room",
                "Maintain oral hydration and balanced electrolytes",
                "Retake vitals scan in 4 hours on your tablet",
                "If pain severity escalates above 5/10, start an instant teleconsult"
            ),
            requiresImmediateDoctor = false,
            isEmergencyFlag = false
        )
    }
}
