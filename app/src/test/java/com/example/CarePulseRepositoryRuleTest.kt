package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.FirestoreHealthRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CarePulseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticated_user_cannot_save_vitals() = runBlocking {
        auth.signOut()
        val repo = FirestoreHealthRepository(firestore)

        try {
            repo.saveVitals(
                heartRate = 72,
                spo2 = 98,
                systolicBp = 120,
                diastolicBp = 80,
                temperatureF = 98.6f,
                respirationRate = 16,
                status = "Normal",
                notes = "Test"
            )
            fail("Expected IllegalStateException for unauthenticated write")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("User must be signed in") == true)
        }
    }

    @Test
    fun authenticated_user_can_save_vitals_to_firestore() = runBlocking {
        val uid = signInTestUser("patient1@carepulse.test")
        val repo = FirestoreHealthRepository(firestore)

        val docId = repo.saveVitals(
            heartRate = 74,
            spo2 = 99,
            systolicBp = 118,
            diastolicBp = 78,
            temperatureF = 98.5f,
            respirationRate = 15,
            status = "Normal",
            notes = "Test authenticated vitals entry"
        )

        assertNotNull(docId)
        assertTrue(docId.isNotEmpty())
    }

    @Test
    fun authenticated_user_can_save_and_retrieve_chat_message() = runBlocking {
        val uid = signInTestUser("patient2@carepulse.test")
        val repo = FirestoreHealthRepository(firestore)

        val msgId = repo.saveChatMessage(
            sender = "Patient",
            message = "Can you analyze my blood pressure trends?",
            isFromUser = true,
            modelUsed = "gemini-3.5-flash"
        )

        assertNotNull(msgId)
        val messages = repo.observeChatMessages().first()
        assertTrue(messages.any { it.message == "Can you analyze my blood pressure trends?" })
    }
}
