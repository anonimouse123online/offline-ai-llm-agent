package com.example.jarvisapp.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import org.json.JSONObject

class AppAction(private val context: Context) {

    companion object {
        const val TAG = "AppAction"
    }

    fun execute(command: JSONObject) {
        val appName = command.optString("app", "").lowercase().trim()
        if (appName.isEmpty()) {
            Log.e(TAG, "No app name in command")
            return
        }
        Log.i(TAG, "Opening app: '$appName'")

        val packageName = findBestMatch(appName)
        if (packageName == null) {
            Log.e(TAG, "No installed app found matching '$appName'")
            return
        }

        val intent = context.packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

        if (intent == null) {
            Log.e(TAG, "No launch intent for $packageName")
            return
        }

        try {
            context.startActivity(intent)
            Log.i(TAG, "Launched: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
        }
    }

    private fun findBestMatch(query: String): String? {
        val pm = context.packageManager

        // Get all launchable apps on this device
        val allApps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            PackageManager.MATCH_ALL
        )

        data class Candidate(val pkg: String, val label: String, val score: Int)

        val candidates = allApps.mapNotNull { info ->
            val label = info.loadLabel(pm).toString().lowercase().trim()
            val pkg   = info.activityInfo.packageName.lowercase()
            val score = scoreMatch(label, pkg, query)
            if (score > 0) Candidate(info.activityInfo.packageName, label, score) else null
        }

        val best = candidates.maxByOrNull { it.score }
        Log.i(TAG, "Best match for '$query': ${best?.label} | ${best?.pkg} | score=${best?.score}")
        return best?.pkg
    }

    private fun scoreMatch(label: String, pkg: String, query: String): Int {
        // Common aliases so "phone" finds dialer, "contacts" finds contacts, etc.
        val aliases = mapOf(
            "phone"     to listOf("dialer", "phone", "call"),
            "contacts"  to listOf("contacts", "people"),
            "messages"  to listOf("messages", "messaging", "sms", "mms"),
            "browser"   to listOf("browser", "chrome", "firefox", "internet"),
            "camera"    to listOf("camera"),
            "gallery"   to listOf("gallery", "photos"),
            "notes"     to listOf("notes", "keep", "memo"),
            "files"     to listOf("files", "file manager", "explorer"),
            "clock"     to listOf("clock", "alarm"),
            "settings"  to listOf("settings")
        )

        // Expand query with aliases
        val queryTerms = mutableSetOf(query)
        aliases[query]?.let { queryTerms.addAll(it) }

        for (term in queryTerms) {
            when {
                label == term              -> return 100
                label.startsWith(term)     -> return 90
                term.startsWith(label)     -> return 85
                label.contains(term)       -> return 70
                pkg.contains(term)         -> return 60
                term.contains(label) && label.length > 3 -> return 55
            }
        }

        // Word-level fuzzy: any query word (>2 chars) appears in label
        val queryWords = query.split(" ").filter { it.length > 2 }
        if (queryWords.any { w -> label.contains(w) || pkg.contains(w) }) return 40

        return 0
    }
}