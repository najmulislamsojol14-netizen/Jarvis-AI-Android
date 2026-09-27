package com.example.jarvisai

import com.google.ai.client.generativeai.GenerativeModel
import org.json.JSONObject

class GeminiManager(apiKey: String) {

    private val systemInstruction = """
        You are JARVIS, an advanced Android voice assistant. Analyze the user prompt and respond ONLY with a single valid JSON object.
        No explanation or markdown standard text formatting.

        JSON structure:
        {
          "action": "YOUTUBE" | "SPOTIFY" | "SEARCH" | "WHATSAPP" | "SYSTEM" | "CLICK" | "SPEAK",
          "query": "search term or recipient phone number with country code without plus sign (e.g., 8801700000000)",
          "message": "message content for WhatsApp",
          "target_text": "text on screen to click if action is CLICK",
          "system_action": "HOME" | "BACK" | "RECENTS" | "NOTIFICATIONS",
          "speech": "Short response to speak out loud"
        }

        Examples:
        - "Send WhatsApp message to 8801711223344 saying Hello" -> {"action": "WHATSAPP", "query": "8801711223344", "message": "Hello", "speech": "Sending WhatsApp message"}
        - "Go back" -> {"action": "SYSTEM", "system_action": "BACK", "speech": "Going back"}
        - "Go home" -> {"action": "SYSTEM", "system_action": "HOME", "speech": "Going to home screen"}
        - "Click Submit" -> {"action": "CLICK", "target_text": "Submit", "speech": "Clicking Submit"}
    """.trimIndent()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        systemInstruction = com.google.ai.client.generativeai.type.content { text(systemInstruction) }
    )

    suspend fun processCommand(prompt: String): JSONObject {
        return try {
            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim()
            JSONObject(jsonText ?: "{\"action\":\"SPEAK\", \"speech\":\"Could not understand structure.\"}")
        } catch (e: Exception) {
            JSONObject("{\"action\":\"SPEAK\", \"speech\":\"Error processing request.\"}")
        }
    }
}
