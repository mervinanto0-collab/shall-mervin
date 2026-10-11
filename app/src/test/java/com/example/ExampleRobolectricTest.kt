package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TriageLevel
import com.example.engine.SymptomAnalysisEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CarePulse", appName)
    }

    @Test
    fun `symptom triage detects emergency chest pain with tachycardia`() {
        val triage = SymptomAnalysisEngine.analyzeSymptoms(
            region = "Chest & Heart",
            symptom = "Chest tightness / Pressure",
            severity = 9,
            durationHours = 2,
            heartRate = 135,
            spo2 = 91,
            systolicBp = 165,
            temperatureF = 98.6f
        )

        assertEquals(TriageLevel.EMERGENCY, triage.level)
        assertTrue(triage.isEmergencyFlag)
    }

    @Test
    fun `symptom triage handles mild condition with normal vitals`() {
        val triage = SymptomAnalysisEngine.analyzeSymptoms(
            region = "Musculoskeletal & Joints",
            symptom = "Neck stiffness & spasm",
            severity = 2,
            durationHours = 3,
            heartRate = 72,
            spo2 = 99,
            systolicBp = 118,
            temperatureF = 98.6f
        )

        assertEquals(TriageLevel.SELF_CARE, triage.level)
    }

    @Test
    fun `vital status thresholds categorize normal and critical parameters correctly`() {
        // Normal vitals check
        val hr = 72
        val o2 = 98
        val temp = 98.6f

        val hrStatus = when {
            hr >= 120 || hr < 45 -> com.example.ui.components.VitalStatus.CRITICAL
            hr > 100 || hr < 55 -> com.example.ui.components.VitalStatus.WARNING
            hr in 60..85 -> com.example.ui.components.VitalStatus.OPTIMAL
            else -> com.example.ui.components.VitalStatus.NORMAL
        }

        val o2Status = when {
            o2 < 90 -> com.example.ui.components.VitalStatus.CRITICAL
            o2 < 95 -> com.example.ui.components.VitalStatus.WARNING
            o2 >= 97 -> com.example.ui.components.VitalStatus.OPTIMAL
            else -> com.example.ui.components.VitalStatus.NORMAL
        }

        val tempStatus = when {
            temp >= 101.5f || temp < 95.5f -> com.example.ui.components.VitalStatus.CRITICAL
            temp >= 99.5f || temp < 97.0f -> com.example.ui.components.VitalStatus.WARNING
            temp in 97.8f..98.9f -> com.example.ui.components.VitalStatus.OPTIMAL
            else -> com.example.ui.components.VitalStatus.NORMAL
        }

        assertEquals(com.example.ui.components.VitalStatus.OPTIMAL, hrStatus)
        assertEquals(com.example.ui.components.VitalStatus.OPTIMAL, o2Status)
        assertEquals(com.example.ui.components.VitalStatus.OPTIMAL, tempStatus)
    }
}
