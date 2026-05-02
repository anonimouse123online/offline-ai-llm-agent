package com.example.jarvisapp

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class BackendClient {

    private val BASE_URL = "http://192.168.1.4:8000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun sendCommand(text: String, callback: (JSONObject) -> Unit) {
        val json = JSONObject().put("text", text).toString()
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/command")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback(JSONObject().put("tool", "unknown"))
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: "{}"
                try {
                    callback(JSONObject(responseBody))
                } catch (e: Exception) {
                    callback(JSONObject().put("tool", "unknown"))
                }
            }
        })
    }
}