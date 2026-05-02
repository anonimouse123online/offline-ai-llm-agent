package com.example.jarvisapp

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

class LlamaEngine(private val context: Context) {

    private var isLoaded = false
    private val modelName = "qwen2.5-0.5b-instruct-q4_k_m.gguf"

    companion object {
        private const val TAG = "LlamaEngine"
        private var libraryLoaded = false

        init {
            try {
                System.loadLibrary("ggml-base")
                System.loadLibrary("ggml-cpu")
                System.loadLibrary("ggml")
                System.loadLibrary("llama")
                System.loadLibrary("llama-common")
                System.loadLibrary("mtmd")
                System.loadLibrary("jarvis_llm")
                libraryLoaded = true
                Log.d(TAG, "jarvis_llm library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                libraryLoaded = false
                Log.e(TAG, "Failed to load jarvis_llm: ${e.message}")
            }
        }
    }
    // ← Removed "private" so JNI can resolve these symbols
    external fun initModel(modelPath: String): Long
    external fun runInference(
        modelHandle: Long,
        systemPrompt: String,
        userInput: String
    ): String
    external fun freeModel(modelHandle: Long)

    private var modelHandle: Long = 0

    val systemPrompt = """
        You are Jarvis, an Android phone controller.
        Reply with ONLY a valid JSON object. No explanation. No extra text.

        Available tools:
        {"tool":"make_call","contact":"name"}
        {"tool":"open_app","app":"facebook|whatsapp|instagram|youtube|settings|camera|gallery"}
        {"tool":"send_message","app":"whatsapp|sms","contact":"name","message":"text"}
        {"tool":"post_facebook","image_index":1,"caption":"text"}
        {"tool":"toggle_setting","setting":"wifi|bluetooth|flashlight|silent","state":"on|off"}
        {"tool":"set_volume","level":0}
        {"tool":"take_screenshot"}
        {"tool":"go_home"}
        {"tool":"go_back"}
        {"tool":"unknown"}

        Examples:
        call John = {"tool":"make_call","contact":"John"}
        open facebook = {"tool":"open_app","app":"facebook"}
        send hi to Maria on whatsapp = {"tool":"send_message","app":"whatsapp","contact":"Maria","message":"hi"}
        turn on flashlight = {"tool":"toggle_setting","setting":"flashlight","state":"on"}
        turn off wifi = {"tool":"toggle_setting","setting":"wifi","state":"off"}
    """.trimIndent()

    fun loadModel(): Boolean {
        if (!libraryLoaded) {
            Log.e(TAG, "Cannot load model — native library failed to load")
            return false
        }
        if (isLoaded) {
            Log.d(TAG, "Model already loaded")
            return true
        }
        return try {
            val modelFile = copyModelFromAssets()
            if (modelFile == null) {
                Log.e(TAG, "Model file not found in assets")
                return false
            }
            Log.d(TAG, "Initializing model at ${modelFile.absolutePath}")
            modelHandle = initModel(modelFile.absolutePath)
            isLoaded = modelHandle != 0L
            if (isLoaded) {
                Log.d(TAG, "Model loaded successfully, handle=$modelHandle")
            } else {
                Log.e(TAG, "initModel returned 0 — check C++ logs")
            }
            isLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}")
            false
        }
    }

    fun processCommand(userInput: String): JSONObject {
        if (!libraryLoaded) {
            Log.e(TAG, "Library not loaded")
            return JSONObject().put("tool", "unknown")
        }
        if (!isLoaded) {
            Log.e(TAG, "Model not loaded")
            return JSONObject().put("tool", "unknown")
        }
        return try {
            val raw = runInference(modelHandle, systemPrompt, userInput)
            Log.d(TAG, "Raw output: $raw")
            extractJson(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
            JSONObject().put("tool", "unknown")
        }
    }

    private fun extractJson(raw: String): JSONObject {
        return try {
            JSONObject(raw.trim())
        } catch (e: Exception) {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start != -1 && end != -1 && end > start) {
                try {
                    JSONObject(raw.substring(start, end + 1))
                } catch (e2: Exception) {
                    Log.w(TAG, "Could not parse JSON from: $raw")
                    JSONObject().put("tool", "unknown")
                }
            } else {
                Log.w(TAG, "No JSON found in output: $raw")
                JSONObject().put("tool", "unknown")
            }
        }
    }

    private fun copyModelFromAssets(): File? {
        val modelFile = File(context.filesDir, modelName)

        if (modelFile.exists() && modelFile.length() > 0) {
            Log.d(TAG, "Model already exists at ${modelFile.absolutePath} (${modelFile.length()} bytes)")
            return modelFile
        }

        return try {
            Log.d(TAG, "Copying model from assets — this may take a moment...")
            context.assets.open(modelName).use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Model copied: ${modelFile.length()} bytes")
            modelFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy model from assets: ${e.message}")
            null
        }
    }

    fun release() {
        if (isLoaded && modelHandle != 0L) {
            try {
                freeModel(modelHandle)
                Log.d(TAG, "Model released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing model: ${e.message}")
            } finally {
                modelHandle = 0
                isLoaded = false
            }
        }
    }
}