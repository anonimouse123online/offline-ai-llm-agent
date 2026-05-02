package com.example.jarvisapp

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        const val TAG = "JarvisAccessibility"
        var instance: JarvisAccessibilityService? = null

        fun clickText(text: String): Boolean {
            return instance?.findAndClick(text) ?: false
        }

        fun typeText(text: String): Boolean {
            return instance?.type(text) ?: false
        }

        fun pressBack() {
            instance?.performGlobalAction(GLOBAL_ACTION_BACK)
        }

        fun pressHome() {
            instance?.performGlobalAction(GLOBAL_ACTION_HOME)
        }

        fun openNotifications() {
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }

        fun isConnected(): Boolean {
            return instance != null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "Accessibility service destroyed")
    }

    fun findAndClick(text: String): Boolean {
        val root = rootInActiveWindow ?: run {
            Log.w(TAG, "No active window")
            return false
        }
        val node = findNodeByText(root, text) ?: run {
            Log.w(TAG, "Node not found: $text")
            return false
        }
        val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        node.recycle()
        Log.i(TAG, "Clicked '$text': $result")
        return result
    }

    fun type(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findFocusedInput(root) ?: run {
            Log.w(TAG, "No focused input found")
            return false
        }
        val args = Bundle()
        args.putString(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )
        val result = node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT, args
        )
        node.recycle()
        Log.i(TAG, "Typed '$text': $result")
        return result
    }

    private fun findNodeByText(
        node: AccessibilityNodeInfo,
        text: String
    ): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            return node
        }
        if (node.contentDescription?.toString()
                ?.contains(text, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    private fun findFocusedInput(
        node: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (node.isFocused && node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFocusedInput(child)
            if (found != null) return found
            child.recycle()
        }
        return null
    }
}