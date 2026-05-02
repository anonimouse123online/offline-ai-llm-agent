package com.example.jarvisapp

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView

class FloatingBubbleManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var bubbleView: android.view.View? = null
    private var params: WindowManager.LayoutParams? = null
    private var isShowing = false

    var onMicClick: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    fun show() {
        if (isShowing) return
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(context)
        bubbleView = inflater.inflate(R.layout.bubble_layout, null)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        // Drag logic
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        bubbleView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (!isDragging && (Math.abs(dx) > 8 || Math.abs(dy) > 8)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params?.x = initialX + dx
                        params?.y = initialY + dy
                        windowManager?.updateViewLayout(bubbleView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    false // pass through to child click listeners
                }
                else -> false
            }
        }

        bubbleView?.findViewById<ImageButton>(R.id.btnMic)?.setOnClickListener {
            onMicClick?.invoke()
        }

        bubbleView?.findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener {
            hide()
            onDismiss?.invoke()
        }

        windowManager?.addView(bubbleView, params)
        isShowing = true
    }

    fun hide() {
        if (!isShowing) return
        try { windowManager?.removeView(bubbleView) } catch (e: Exception) { e.printStackTrace() }
        bubbleView = null
        isShowing = false
    }

    fun dismiss() = hide()

    fun updateStatus(text: String) {
        bubbleView?.findViewById<TextView>(R.id.tvJarvisText)?.text = text
    }

    fun isVisible() = isShowing
}