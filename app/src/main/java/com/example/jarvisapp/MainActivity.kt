package com.example.jarvisapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.SEND_SMS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true
        Log.i(TAG, "Permissions result: mic=$micGranted")
        if (micGranted) {
            checkOverlayPermission()
        } else {
            Log.e(TAG, "Microphone permission denied — Jarvis cannot work")
        }
    }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            Log.i(TAG, "Overlay permission granted")
        } else {
            Log.e(TAG, "Overlay permission denied — bubble will not show")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JarvisScreen(
                        onStartService = { startJarvisService() },
                        onStopService = { stopJarvisService() }
                    )
                }
            }
        }
        requestPermissions()
    }

    private fun requestPermissions() {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            Log.i(TAG, "Requesting missing permissions: $missing")
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            checkOverlayPermission()
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Log.i(TAG, "Requesting overlay permission")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayLauncher.launch(intent)  // use launcher so we get a callback
        } else {
            Log.i(TAG, "Overlay permission already granted")
        }
    }

    private fun startJarvisService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot start — mic permission not granted")
            requestPermissions()
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Cannot start — overlay permission not granted")
            checkOverlayPermission()
            return
        }

        Log.i(TAG, "Starting JarvisService")
        val intent = Intent(this, JarvisService::class.java).apply {
            action = "START_JARVIS"  // ✅ explicit action
        }
        startForegroundService(intent)
    }

    private fun stopJarvisService() {
        Log.i(TAG, "Stopping JarvisService")
        val intent = Intent(this, JarvisService::class.java).apply {
            action = "STOP_JARVIS"
        }
        startService(intent)  // sends STOP_JARVIS action to onStartCommand
    }
}

@Composable
fun JarvisScreen(onStartService: () -> Unit, onStopService: () -> Unit) {
    var status by remember { mutableStateOf("Jarvis is ready") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🤖 Jarvis AI", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            onStartService()
            status = "Jarvis is running..."
        }) {
            Text("Start Jarvis")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = {
            onStopService()
            status = "Jarvis stopped"
        }) {
            Text("Stop Jarvis")
        }
    }
}