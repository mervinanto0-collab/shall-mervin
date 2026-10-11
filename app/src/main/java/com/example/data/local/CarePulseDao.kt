package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DoctorConsultation
import com.example.data.model.MedicationItem
import com.example.data.model.SymptomReport
import com.example.data.model.VitalsRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CarePulseDao {

    // Vitals
    @Query("SELECT * FROM vitals_records ORDER BY timestamp DESC")
    fun getAllVitals(): Flow<List<VitalsRecord>>

    @Query("SELECT * FROM vitals_records ORDER BY timestamp DESC LIMIT 1")
    fun getLatestVitals(): Flow<VitalsRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitals(vitals: VitalsRecord): Long

    // Symptom Reports
    @Query("SELECT * FROM symptom_reports ORDER BY timestamp DESC")
    fun getAllSymptomReports(): Flow<List<SymptomReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomReport(report: SymptomReport): Long

    // Consultations
    @Query("SELECT * FROM doctor_consultations ORDER BY timestamp DESC")
    fun getAllConsultations(): Flow<List<DoctorConsultation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsultation(consultation: DoctorConsultation): Long

    // Medications
    @Query("SELECT * FROM medications ORDER BY id ASC")
    fun getAllMedications(): Flow<List<MedicationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedications(medications: List<MedicationItem>)

    @Update
    suspend fun updateMedication(medication: MedicationItem)

    @Query("UPDATE medications SET isTaken = :isTaken WHERE id = :id")
    suspend fun setMedicationTaken(id: Long, isTaken: Boolean)

    @Query("SELECT COUNT(*) FROM medications")
    suspend fun getMedicationCount(): Int
}
