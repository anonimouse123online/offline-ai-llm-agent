package com.example.jarvisapp.actions

import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.hardware.camera2.CameraManager
import org.json.JSONObject

class SettingsAction(private val context: Context) {

    fun execute(command: JSONObject) {
        val setting = command.optString("setting", "")
        val state = command.optString("state", "on") == "on"

        when (setting) {
            "wifi"       -> toggleWifi(state)
            "bluetooth"  -> toggleBluetooth(state)
            "flashlight" -> toggleFlashlight(state)
            "silent"     -> toggleSilent(state)
        }
    }

    fun setVolume(command: JSONObject) {
        val level = command.optInt("level", 5)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volume = (level.toFloat() / 10 * max).toInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    private fun toggleWifi(enable: Boolean) {
        try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enable
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleBluetooth(enable: Boolean) {
        try {
            val bt = BluetoothAdapter.getDefaultAdapter()
            if (enable) bt?.enable() else bt?.disable()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleFlashlight(enable: Boolean) {
        try {
            val camera = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = camera.cameraIdList[0]
            camera.setTorchMode(cameraId, enable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleSilent(enable: Boolean) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.ringerMode = if (enable)
            AudioManager.RINGER_MODE_SILENT
        else
            AudioManager.RINGER_MODE_NORMAL
    }
}