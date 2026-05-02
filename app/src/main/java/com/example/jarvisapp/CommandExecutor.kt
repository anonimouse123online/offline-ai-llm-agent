package com.example.jarvisapp

import android.content.Context
import com.example.jarvisapp.actions.*
import org.json.JSONObject
import android.util.Log

class CommandExecutor(private val context: Context) {

    companion object {
        const val TAG = "CommandExecutor"
    }

    fun execute(command: JSONObject) {
        val tool = command.optString("tool")
        Log.i(TAG, "Executing tool: $tool")

        when (tool) {
            "make_call"         -> CallAction(context).execute(command)
            "open_app"          -> AppAction(context).execute(command)
            "send_message"      -> WhatsAppAction(context).execute(command)
            "post_facebook"     -> FacebookAction(context).execute(command)
            "toggle_setting"    -> SettingsAction(context).execute(command)
            "set_volume"        -> SettingsAction(context).setVolume(command)
            "go_home"           -> JarvisAccessibilityService.pressHome()
            "go_back"           -> JarvisAccessibilityService.pressBack()
            "open_notifications"-> JarvisAccessibilityService.openNotifications()
            "click"             -> {
                val target = command.optString("target", "")
                if (target.isNotBlank()) {
                    val success = JarvisAccessibilityService.clickText(target)
                    Log.i(TAG, "Click '$target': $success")
                }
            }
            "type_text"         -> {
                val text = command.optString("text", "")
                if (text.isNotBlank()) {
                    val success = JarvisAccessibilityService.typeText(text)
                    Log.i(TAG, "Type '$text': $success")
                }
            }
            "navigate"          -> {
                val params = mapOf(
                    "destination" to command.optString("destination"),
                    "query" to command.optString("query")
                )
                NavigationAction(context).execute(params)
            }
            else                -> Log.w(TAG, "Unknown command: $tool")
        }
    }
}