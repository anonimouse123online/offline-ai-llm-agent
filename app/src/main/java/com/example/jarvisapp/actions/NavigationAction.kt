package com.example.jarvisapp.actions

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.jarvisapp.JarvisAccessibilityService

class NavigationAction(private val context: Context) {

    fun execute(params: Map<String, String>): String {
        val destination = params["destination"] ?: params["query"] ?: "unknown"
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Navigating to $destination"
        } catch (e: Exception) {
            try {
                val mapsIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://maps.google.com/?q=${Uri.encode(destination)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(mapsIntent)
                "Opening maps for $destination"
            } catch (e2: Exception) {
                "Navigation failed: ${e2.message}"
            }
        }
    }

    fun goHome() {
        Log.i("NavigationAction", "Going home")
        try {
            JarvisAccessibilityService.instance?.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_HOME
            ) ?: run {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("NavigationAction", "goHome failed: ${e.message}")
        }
    }

    fun goBack() {
        Log.i("NavigationAction", "Going back")
        try {
            JarvisAccessibilityService.instance?.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_BACK
            ) ?: Log.w("NavigationAction", "Accessibility service not available")
        } catch (e: Exception) {
            Log.e("NavigationAction", "goBack failed: ${e.message}")
        }
    }
}