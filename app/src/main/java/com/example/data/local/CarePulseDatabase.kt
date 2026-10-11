package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.DoctorConsultation
import com.example.data.model.MedicationItem
import com.example.data.model.SymptomReport
import com.example.data.model.VitalsRecord

@Database(
    entities = [
        VitalsRecord::class,
        SymptomReport::class,
        DoctorConsultation::class,
        MedicationItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CarePulseDatabase : RoomDatabase() {
    abstract fun dao(): CarePulseDao

    companion object {
        @Volatile
        private var INSTANCE: CarePulseDatabase? = null

        fun getDatabase(context: Context): CarePulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarePulseDatabase::class.java,
                    "carepulse_health_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
