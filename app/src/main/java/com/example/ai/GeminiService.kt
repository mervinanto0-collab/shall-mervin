package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class AiClinicalRole(
    val title: String,
    val modelId: String,
    val description: String,
    val systemInstruction: String
) {
    GENERAL_DOCTOR(
        title = "Dr. CarePulse AI",
        modelId = "gemini-3.5-flash",
        description = "General Health Guidance & Triage",
        systemInstruction = "You are Dr. CarePulse AI, a compassionate clinical tele-triage physician integrated into a patient's smart tablet health monitor. Your role is to provide clear, evidence-based medical explanations, guide the patient through their symptoms, explain vital telemetry, and recommend appropriate care. Format key advice clearly with bullet points. Always advise activating Emergency SOS or calling 911 if red flags appear."
    ),
    COMPLEX_DIAGNOSTIC(
        title = "Specialist Diagnostic",
        modelId = "gemini-3.1-pro-preview",
        description = "In-depth Clinical Differential Reasoning",
        systemInstruction = "You are an advanced Clinical Diagnostic Specialist. Your role is complex medical reasoning: analyzing multi-system symptoms, cross-referencing chronic comorbidities, medication interactions, and vital telemetry parameters. Provide structured differential diagnoses, pathophysiological context, test recommendations, and specific clinical questions for their physician."
    ),
    RAPID_TRIAGE(
        title = "Rapid Tele-Triage",
        modelId = "gemini-3.1-flash-lite-preview",
        description = "Instant Emergency & Safety Screening",
        systemInstruction = "You are a Rapid Emergency Tele-Triage screener. Your role is fast, direct, and concise safety assessment. Screen for immediate life threats (acute coronary syndrome, stroke, severe respiratory compromise). Provide urgent categorical triage (Emergency, Urgent Teleconsult, or Safe Self-Care) with clear immediate next steps in under 3 short paragraphs."
    )
}

data class ChatTurn(
    val role: String, // "user" or "model"
    val text: String
)

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateChatResponse(
        role: AiClinicalRole,
        history: List<ChatTurn>,
        latestUserMessage: String,
        patientVitalsContext: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Clinical AI Assistant is running in demonstration mode. Please configure your GEMINI_API_KEY in the Secrets panel in AI Studio to enable full live neural reasoning."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${role.modelId}:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // Append historical turns
        for (turn in history.takeLast(10)) {
            val contentObj = JSONObject().apply {
                put("role", turn.role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", turn.text) })
                })
            }
            contentsArray.put(contentObj)
        }

        // Prepare current user message with vitals context if present
        val promptText = if (!patientVitalsContext.isNullOrBlank()) {
            "[PATIENT LIVE TELEMETRY SNAPSHOT]: $patientVitalsContext\n\n$latestUserMessage"
        } else {
            latestUserMessage
        }

        val userObj = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", promptText) })
            })
        }
        contentsArray.put(userObj)

        val rootJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", role.systemInstruction) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.4)
                put("topP", 0.95)
            })
        }

        val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errMsg = try {
                        val errJson = JSONObject(bodyStr)
                        errJson.optJSONObject("error")?.optString("message") ?: response.message
                    } catch (e: Exception) {
                        response.message
                    }
                    return@withContext "Clinical AI Service response: $errMsg"
                }

                val resJson = JSONObject(bodyStr)
                val candidates = resJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                text ?: "I have processed your query. Please review your tablet vitals and consult an on-duty doctor if symptoms persist."
            }
        } catch (e: Exception) {
            "Unable to reach Gemini Clinical AI service (${e.localizedMessage}). Please check network connectivity or consult an on-duty doctor directly."
        }
    }
}
