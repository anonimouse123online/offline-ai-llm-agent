package com.example.jarvisapp.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

class WhatsAppAction(private val context: Context) {
    fun execute(command: JSONObject) {
        val contact = command.optString("contact", "")
        val message = command.optString("message", "")
        val app = command.optString("app", "whatsapp")

        if (app == "sms") {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$contact")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(intent) }
            catch (e: Exception) { e.printStackTrace() }
        } else {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(
                    "https://api.whatsapp.com/send?phone=$contact&text=${
                        Uri.encode(message)
                    }"
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(intent) }
            catch (e: Exception) { e.printStackTrace() }
        }
    }
}