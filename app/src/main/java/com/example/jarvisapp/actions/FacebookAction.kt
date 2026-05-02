package com.example.jarvisapp.actions

import android.content.Context
import android.content.Intent
import org.json.JSONObject

class FacebookAction(private val context: Context) {
    fun execute(command: JSONObject) {
        // Opens Facebook — full post automation
        // requires Accessibility Service
        val intent = context.packageManager
            .getLaunchIntentForPackage("com.facebook.katana")?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        try {
            if (intent != null) context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}