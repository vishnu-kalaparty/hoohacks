package com.example.cadence

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BreatheActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_breathe)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvCountdown = findViewById<TextView>(R.id.tvCountdown)
        val breatheOuter = findViewById<FrameLayout>(R.id.breatheOuter)
        val breatheMiddle = findViewById<FrameLayout>(R.id.breatheMiddle)
        val breatheInner = findViewById<FrameLayout>(R.id.breatheInner)
        val tvBreatheLabel = findViewById<TextView>(R.id.tvBreatheLabel)

        // Start breathing animation
        startBreathingAnimation(breatheOuter, breatheMiddle, breatheInner, tvBreatheLabel)

        // Start 30-second countdown
        countDownTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvCountdown.text = "${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                tvCountdown.text = "0"
                val resultsIntent = Intent(this@BreatheActivity, ResultsActivity::class.java)
                resultsIntent.putExtra("total_score", intent.getIntExtra("total_score", 0))
                resultsIntent.putExtra("screening_type",
                    intent.getStringExtra("screening_type") ?: SelectScreeningActivity.TYPE_PHQ9)
                // Pass through follow-up answers
                resultsIntent.putExtra("followup_distress", intent.getIntExtra("followup_distress", -1))
                resultsIntent.putExtra("followup_stressful", intent.getBooleanExtra("followup_stressful", false))
                resultsIntent.putExtra("followup_stress_desc", intent.getStringExtra("followup_stress_desc") ?: "")
                resultsIntent.putExtra("followup_coping", intent.getStringExtra("followup_coping") ?: "")
                startActivity(resultsIntent)
                finish()
            }
        }.start()

        // Start camera - TEMPORARILY DISABLED FOR DEBUGGING
        // if (checkCameraPermission()) {
        //     startCamera()
        // } else {
        //     requestCameraPermission()
        // }
    }

    private fun startBreathingAnimation(
        outer: FrameLayout,
        middle: FrameLayout,
        inner: FrameLayout,
        label: TextView
    ) {
        // Inhale: scale up over 4 seconds
        val inhaleOuter = ObjectAnimator.ofFloat(outer, View.SCALE_X, 1f, 1.1f).apply { duration = 4000 }
        val inhaleOuterY = ObjectAnimator.ofFloat(outer, View.SCALE_Y, 1f, 1.1f).apply { duration = 4000 }
        val inhaleMiddle = ObjectAnimator.ofFloat(middle, View.SCALE_X, 1f, 1.08f).apply { duration = 4000 }
        val inhaleMiddleY = ObjectAnimator.ofFloat(middle, View.SCALE_Y, 1f, 1.08f).apply { duration = 4000 }
        val inhaleInner = ObjectAnimator.ofFloat(inner, View.SCALE_X, 1f, 1.05f).apply { duration = 4000 }
        val inhaleInnerY = ObjectAnimator.ofFloat(inner, View.SCALE_Y, 1f, 1.05f).apply { duration = 4000 }

        // Exhale: scale back down over 4 seconds
        val exhaleOuter = ObjectAnimator.ofFloat(outer, View.SCALE_X, 1.1f, 1f).apply { duration = 4000 }
        val exhaleOuterY = ObjectAnimator.ofFloat(outer, View.SCALE_Y, 1.1f, 1f).apply { duration = 4000 }
        val exhaleMiddle = ObjectAnimator.ofFloat(middle, View.SCALE_X, 1.08f, 1f).apply { duration = 4000 }
        val exhaleMiddleY = ObjectAnimator.ofFloat(middle, View.SCALE_Y, 1.08f, 1f).apply { duration = 4000 }
        val exhaleInner = ObjectAnimator.ofFloat(inner, View.SCALE_X, 1.05f, 1f).apply { duration = 4000 }
        val exhaleInnerY = ObjectAnimator.ofFloat(inner, View.SCALE_Y, 1.05f, 1f).apply { duration = 4000 }

        val inhaleSet = AnimatorSet().apply {
            playTogether(inhaleOuter, inhaleOuterY, inhaleMiddle, inhaleMiddleY, inhaleInner, inhaleInnerY)
            interpolator = AccelerateDecelerateInterpolator()
        }

        val exhaleSet = AnimatorSet().apply {
            playTogether(exhaleOuter, exhaleOuterY, exhaleMiddle, exhaleMiddleY, exhaleInner, exhaleInnerY)
            interpolator = AccelerateDecelerateInterpolator()
        }

        val breatheCycle = AnimatorSet().apply {
            playSequentially(inhaleSet, exhaleSet)
        }

        breatheCycle.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Update label text for breathing guidance
                breatheCycle.start()
            }
        })

        // Also animate the label text
        inhaleSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: android.animation.Animator) {
                label.text = "Breathe In"
            }
        })
        exhaleSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: android.animation.Animator) {
                label.text = "Breathe Out"
            }
        })

        breatheCycle.start()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                val previewView = findViewById<PreviewView>(R.id.breatheCameraPreview)
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                Log.e("BreatheActivity", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, g: IntArray) {
        super.onRequestPermissionsResult(rc, p, g)
        if (rc == 101 && g.isNotEmpty() && g[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
