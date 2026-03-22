package com.example.cadence

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.presagetech.smartspectra.SmartSpectraSdk

class CameraTestActivity : AppCompatActivity() {

    private lateinit var tvHeartRate: TextView
    private lateinit var tvRespRate: TextView
    private lateinit var tvOxygen: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_test)

        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvRespRate = findViewById(R.id.tvRespRate)
        tvOxygen = findViewById(R.id.tvOxygen)
        val btnLaunch = findViewById<Button>(R.id.btnLaunchCamera)

        btnLaunch.setOnClickListener {
            if (checkPermissions()) {
                launchPresageCamera()
            } else {
                requestPermissions()
            }
        }
        
        // Setup SDK initial config
        try {
            SmartSpectraSdk.initialize(this.applicationContext)
            val sdk = SmartSpectraSdk.getInstance()
            sdk.setApiKey("ExO2F77fHN9MPSCXnmTt67TcPrKi0tc5aqSJK63Z")
            sdk.setMeasurementDuration(30.0)

            sdk.setEdgeMetricsObserver { metrics ->
                runOnUiThread {
                    Log.d("PresageTest", "Received Metrics: $metrics")
                    // The metrics object usually has fields like pulseRate, breathingRate, oxygenSaturation
                    // Since we are debugging, we'll try to extract them safely.
                    // If metrics is a Map or a specific object, adjust accordingly.
                    val metricsStr = metrics.toString()
                    
                    // Simple parser for common SDK outputs if it's a string
                    if (metricsStr.contains("pulseRate")) {
                        tvHeartRate.text = extractValue(metricsStr, "pulseRate")
                    }
                    if (metricsStr.contains("breathingRate")) {
                        tvRespRate.text = extractValue(metricsStr, "breathingRate")
                    }
                    if (metricsStr.contains("oxygenSaturation")) {
                        tvOxygen.text = extractValue(metricsStr, "oxygenSaturation")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PresageTest", "Initial Setup failed", e)
        }
    }

    private fun extractValue(data: String, key: String): String {
        return try {
            val start = data.indexOf(key) + key.length + 1
            var end = data.indexOf(",", start)
            if (end == -1) end = data.indexOf(")", start)
            if (end == -1) end = data.length
            data.substring(start, end).replace("=", "").trim()
        } catch (e: Exception) {
            "--"
        }
    }

    private fun launchPresageCamera() {
        try {
            val intent = Intent()
            intent.setClassName(this, "com.presagetech.smartspectra.ui.SmartSpectraActivity")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("PresageTest", "Launch failed", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(rc, p, g)
        if (rc == 101 && g.isNotEmpty() && g[0] == PackageManager.PERMISSION_GRANTED) {
            launchPresageCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
        }
    }
}