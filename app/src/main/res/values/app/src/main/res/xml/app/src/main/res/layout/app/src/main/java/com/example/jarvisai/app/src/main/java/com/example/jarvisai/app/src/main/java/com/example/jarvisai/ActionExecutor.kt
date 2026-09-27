package com.example.jarvisai

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

class ActionExecutor(private val context: Context) {

    fun execute(response: JSONObject) {
        val action = response.optString("action", "SPEAK")
        val query = response.optString("query", "")
        val message = response.optString("message", "")
        val systemAction = response.optString("system_action", "")
        val targetText = response.optString("target_text", "")

        val service = JarvisAccessibilityService.instance

        when (action) {
            "WHATSAPP" -> {
                if (query.isNotEmpty() && message.isNotEmpty()) {
                    JarvisAccessibilityService.pendingWhatsAppMessage = message
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$query"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
            "SYSTEM" -> {
                service?.performGlobalSystemAction(systemAction)
            }
            "CLICK" -> {
                if (targetText.isNotEmpty()) {
                    service?.clickByText(targetText)
                }
            }
            "YOUTUBE" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            "SPOTIFY" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$query"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$query"))
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                }
            }
            "SEARCH" -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }
}
