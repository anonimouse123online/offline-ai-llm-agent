package com.example.jarvisapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

class JarvisService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var bubbleManager: FloatingBubbleManager
    private lateinit var llamaEngine: LlamaEngine
    private lateinit var commandExecutor: CommandExecutor
    private lateinit var speechRecognizer: SpeechRecognizer
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isTtsReady = false
    private var isListening = false

    companion object {
        const val CHANNEL_ID = "jarvis_service"
        const val NOTIFICATION_ID = 1
        const val TAG = "JarvisService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: service starting")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Tap the bubble to speak"))

        tts = TextToSpeech(this, this)
        bubbleManager = FloatingBubbleManager(this)
        llamaEngine = LlamaEngine(this)
        commandExecutor = CommandExecutor(this)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()

        bubbleManager.onMicClick = {
            mainHandler.post { startListening() }
        }

        Thread { llamaEngine.loadModel() }.start()

        // Show bubble immediately on start
        bubbleManager.show()

        Log.i(TAG, "onCreate: complete")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            "STOP_JARVIS" -> stopSelf()
        }
        return START_STICKY
    }

    private fun startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening, ignoring")
            return
        }
        Log.i(TAG, "startListening")
        isListening = true
        speak("Yes sir?")
        bubbleManager.updateStatus("Listening...")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        }
        speechRecognizer.startListening(intent)
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: android.os.Bundle?) {
                Log.i(TAG, "onReadyForSpeech")
                bubbleManager.updateStatus("Listening...")
            }

            override fun onBeginningOfSpeech() {
                Log.i(TAG, "onBeginningOfSpeech")
                bubbleManager.updateStatus("Hearing you...")
            }

            override fun onEndOfSpeech() {
                Log.i(TAG, "onEndOfSpeech")
                isListening = false
                bubbleManager.updateStatus("Processing...")
            }

            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()

                Log.i(TAG, "onResults: heard='$text'")

                if (text.isNullOrBlank()) {
                    bubbleManager.updateStatus("Didn't catch that")
                    mainHandler.postDelayed({
                        bubbleManager.updateStatus("Tap to speak")
                    }, 1500)
                    return
                }

                bubbleManager.updateStatus("Processing: $text")

                Thread {
                    try {
                        Log.i(TAG, "Sending to LLM: $text")
                        val command = llamaEngine.processCommand(text)
                        Log.i(TAG, "LLM output: $command")
                        commandExecutor.execute(command)
                        val tool = command.optString("tool", "unknown")
                        Log.i(TAG, "Executed tool: $tool")
                        mainHandler.post { bubbleManager.updateStatus("Done: $tool") }
                        Thread.sleep(2000)
                        mainHandler.post { bubbleManager.updateStatus("Tap to speak") }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing command", e)
                        mainHandler.post { bubbleManager.updateStatus("Error: ${e.message}") }
                        Thread.sleep(2000)
                        mainHandler.post { bubbleManager.updateStatus("Tap to speak") }
                    }
                }.start()
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "No mic permission"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Error $error"
                }
                Log.e(TAG, "onError: $errorMsg (code=$error)")
                bubbleManager.updateStatus(errorMsg)
                mainHandler.postDelayed({
                    bubbleManager.updateStatus("Tap to speak")
                }, 1500)
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
    }

    private fun speak(text: String) {
        if (isTtsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            isTtsReady = true
            Log.i(TAG, "TTS ready")
        }
    }

    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Jarvis Service", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Jarvis background service" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String = "Jarvis is active"): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        speechRecognizer.destroy()
        tts.shutdown()
        bubbleManager.dismiss()
        llamaEngine.release()
    }
}