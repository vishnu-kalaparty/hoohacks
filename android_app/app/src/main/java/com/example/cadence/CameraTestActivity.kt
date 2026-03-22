package com.example.cadence

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.presagetech.smartspectra.SmartSpectraSdk

class CameraTestActivity : AppCompatActivity() {

    private lateinit var tvHeartRate: TextView
    private lateinit var tvRespRate: TextView
    private var sdk: SmartSpectraSdk? = null
    private var isRecordingSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_test)

        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvRespRate = findViewById(R.id.tvRespRate)
        val btnLaunch = findViewById<Button>(R.id.btnLaunchCamera)

        btnLaunch.setOnClickListener {
            if (checkPermissions()) {
                launchPresageCamera()
            } else {
                requestPermissions()
            }
        }

        setupPresageSDK()
    }

    private fun setupPresageSDK() {
        try {
            SmartSpectraSdk.initialize(this.applicationContext)
            sdk = SmartSpectraSdk.getInstance()
            sdk?.setApiKey("ExO2F77fHN9MPSCXnmTt67TcPrKi0tc5aqSJK63Z")
            sdk?.setMeasurementDuration(20.0) // Set to 20 seconds

            // Metrics Buffer Observer (Accurate Final Results)
            sdk?.setMetricsBufferObserver { buffer ->
                runOnUiThread {
                    Log.d("PresageTest", "Final Buffer Received!")

                    // Final Heart Rate from buffer
                    if (buffer.hasPulse() && buffer.pulse.rateCount > 0) {
                        val pulse = buffer.pulse.getRate(buffer.pulse.rateCount - 1).value
                        tvHeartRate.text = String.format("%.0f", pulse)
                        Log.i("PresageTest", "Final HR: $pulse")
                    }

                    // Final Respiration Rate from buffer
                    if (buffer.hasBreathing() && buffer.breathing.rateCount > 0) {
                        val resp = buffer.breathing.getRate(buffer.breathing.rateCount - 1).value
                        tvRespRate.text = String.format("%.0f", resp)
                        Log.i("PresageTest", "Final Resp: $resp")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PresageTest", "Setup failed", e)
        }
    }

    private val presageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // No action needed here as the observer handles data
        Log.d("PresageTest", "Camera Activity Closed")
    }

    private fun launchPresageCamera() {
        try {
            val intent = Intent()
            intent.setClassName(this, "com.presagetech.smartspectra.ui.SmartSpectraActivity")
            presageLauncher.launch(intent)
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
        }
    }
}
