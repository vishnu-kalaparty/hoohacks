package com.example.cadence

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.presagetech.smartspectra.SmartSpectra

class CameraTestActivity : AppCompatActivity() {

    private lateinit var tvResults: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_test)

        tvResults = findViewById(R.id.tvResults)
        val btnLaunch = findViewById<Button>(R.id.btnLaunchCamera)

        btnLaunch.setOnClickListener {
            if (checkPermissions()) {
                startPresageCapture()
            } else {
                requestPermissions()
            }
        }
    }

    private fun startPresageCapture() {
        Log.d("PresageTest", "Starting Camera Capture...")

        // This launches the SDK's built-in camera UI
        SmartSpectra.launchCamera(this) { result ->
            // This callback runs when the SDK finishes processing
            runOnUiThread {
                val resultText = "Capture Success: ${result.isSuccess}\nData: ${result.dataSummary}"
                tvResults.text = resultText

                // Print to Logcat (Terminal)
                Log.d("PresageTest", "RESULT RECEIVED: $resultText")
                Log.d("PresageTest", "RAW JSON: ${result.rawJson}")
            }
        }
    }

    private fun checkPermissions() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
    }
}