package com.example.data.repository

import com.example.data.local.CarePulseDao
import com.example.data.model.DoctorConsultation
import com.example.data.model.MedicationItem
import com.example.data.model.SymptomReport
import com.example.data.model.VitalsRecord
import kotlinx.coroutines.flow.Flow

class HealthRepository(private val dao: CarePulseDao) {

    val allVitals: Flow<List<VitalsRecord>> = dao.getAllVitals()
    val latestVitals: Flow<VitalsRecord?> = dao.getLatestVitals()
    val allSymptomReports: Flow<List<SymptomReport>> = dao.getAllSymptomReports()
    val allConsultations: Flow<List<DoctorConsultation>> = dao.getAllConsultations()
    val allMedications: Flow<List<MedicationItem>> = dao.getAllMedications()

    suspend fun recordVitals(vitals: VitalsRecord): Long {
        return dao.insertVitals(vitals)
    }

    suspend fun saveSymptomReport(report: SymptomReport): Long {
        return dao.insertSymptomReport(report)
    }

    suspend fun saveConsultation(consultation: DoctorConsultation): Long {
        return dao.insertConsultation(consultation)
    }

    suspend fun setMedicationTaken(id: Long, isTaken: Boolean) {
        dao.setMedicationTaken(id, isTaken)
    }

    suspend fun seedInitialDataIfEmpty() {
        if (dao.getMedicationCount() == 0) {
            val initialMeds = listOf(
                MedicationItem(
                    name = "Lisinopril",
                    dosage = "10mg Tablet",
                    timing = "Morning 8:00 AM",
                    isTaken = true,
                    instructions = "Take once daily with a full glass of water for blood pressure",
                    prescribedBy = "Dr. Sarah Chen"
                ),
                MedicationItem(
                    name = "Metformin HCl",
                    dosage = "500mg Tablet",
                    timing = "Morning with breakfast",
                    isTaken = true,
                    instructions = "Take with meal to support metabolic balance",
                    prescribedBy = "Dr. Marcus Vance"
                ),
                MedicationItem(
                    name = "Atorvastatin",
                    dosage = "20mg Tablet",
                    timing = "Evening 8:00 PM",
                    isTaken = false,
                    instructions = "Take at bedtime with water",
                    prescribedBy = "Dr. Marcus Vance"
                ),
                MedicationItem(
                    name = "Vitamin D3 + K2",
                    dosage = "2000 IU Capsule",
                    timing = "Morning 8:00 AM",
                    isTaken = false,
                    instructions = "Immune and cardiovascular support supplement",
                    prescribedBy = "Dr. Sarah Chen"
                )
            )
            dao.insertMedications(initialMeds)

            // Seed initial baseline vitals
            val baselineTime = System.currentTimeMillis() - (1000L * 60 * 60 * 4)
            dao.insertVitals(
                VitalsRecord(
                    timestamp = baselineTime,
                    heartRate = 72,
                    spo2 = 98,
                    systolicBp = 120,
                    diastolicBp = 78,
                    temperatureF = 98.6f,
                    respirationRate = 16,
                    status = "Normal",
                    notes = "Resting baseline reading via Bluetooth Pulse Oximeter & BP Cuff"
                )
            )

            // Seed a previous teleconsult record for history
            val consultTime = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 3)
            dao.insertConsultation(
                DoctorConsultation(
                    timestamp = consultTime,
                    doctorName = "Dr. Sarah Chen",
                    doctorSpecialty = "Internal Medicine & Tele-Triage",
                    chiefComplaint = "Routine remote wellness check-in and blood pressure review",
                    clinicalSummary = "Patient vitals tele-monitored. BP 122/80 mmHg, SpO2 98%. Mild seasonal congestion noted, clear lung sounds on digital stethoscope.",
                    prescriptionSummary = "Continue Lisinopril 10mg daily. Hydrate with electrolyte fluids.",
                    followUpAdvice = "Monitor morning blood pressure on tablet health station daily.",
                    callDurationMinutes = 14
                )
            )
        }
    }
}
