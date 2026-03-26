package com.example.cadence

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.presagetech.smartspectra.SmartSpectraSdk

class ScreeningActivity : AppCompatActivity() {

    private var currentQuestion = 0
    private var totalQuestions = 0
    private lateinit var scores: IntArray
    private lateinit var questions: Array<String>
    private lateinit var screeningTitle: String

    private lateinit var tvQuestionCounter: TextView
//    private lateinit var tvQuestionLabel: TextView
    private lateinit var tvQuestionText: TextView
    private lateinit var progressFill: View
    private lateinit var answerCards: Array<LinearLayout>
    private lateinit var radioButtons: Array<RadioButton>
    private lateinit var btnSubmit: TextView
    private var selectedAnswer = -1

    private var sdk: SmartSpectraSdk? = null

    // --- THE HACKY FIX: THE LOCK ---
    // This prevents the "Buffer Received" from moving the question twice
    private var isWaitingForData = false

    companion object {
        private val PHQ9_QUESTIONS = arrayOf(
            "Over the last 2 weeks, how often have you been bothered by little interest or pleasure in doing things?",
            "Over the last 2 weeks, how often have you been bothered by feeling down, depressed, or hopeless?",
            "Over the last 2 weeks, how often have you been bothered by trouble falling or staying asleep, or sleeping too much?",
            "Over the last 2 weeks, how often have you been bothered by feeling tired or having little energy?",
            "Over the last 2 weeks, how often have you been bothered by poor appetite or overeating?",
            "Over the last 2 weeks, how often have you been bothered by feeling bad about yourself — or that you are a failure or have let yourself or your family down?",
            "Over the last 2 weeks, how often have you been bothered by trouble concentrating on things, such as reading the newspaper or watching television?",
            "Over the last 2 weeks, how often have you been bothered by moving or speaking so slowly that other people could have noticed? Or the opposite — being so fidgety or restless that you have been moving around a lot more than usual?",
            "Over the last 2 weeks, how often have you been bothered by thoughts that you would be better off dead, or of hurting yourself in some way?"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screening)

        setupUI()
        setupPresageSDK()
        displayQuestion()
    }

    private fun setupUI() {
        questions = PHQ9_QUESTIONS
        totalQuestions = questions.size
        scores = IntArray(totalQuestions)

        tvQuestionCounter = findViewById(R.id.tvQuestionCounter)
//        tvQuestionLabel = findViewById(R.id.tvQuestionLabel)
        tvQuestionText = findViewById(R.id.tvQuestionText)
        progressFill = findViewById(R.id.screeningProgressFill)
        btnSubmit = findViewById(R.id.btnSubmitAnswer)

        answerCards = arrayOf(findViewById(R.id.answer0), findViewById(R.id.answer1), findViewById(R.id.answer2), findViewById(R.id.answer3))
        radioButtons = arrayOf(findViewById(R.id.radio0), findViewById(R.id.radio1), findViewById(R.id.radio2), findViewById(R.id.radio3))

        for (i in answerCards.indices) {
            answerCards[i].setOnClickListener { selectAnswer(i) }
        }

        btnSubmit.setOnClickListener {
            if (selectedAnswer < 0) return@setOnClickListener

            // Start the sequence
            isWaitingForData = true
            if (checkPermissions()) {
                launchPresageCamera()
            } else {
                requestPermissions()
            }
        }
    }

    private fun setupPresageSDK() {
        try {
            SmartSpectraSdk.initialize(this.applicationContext)
            sdk = SmartSpectraSdk.getInstance()
            sdk?.setApiKey("q3aQxmrSky5XLokq8Js0Q3OcTkzsJ6244fz3gzLw")
            sdk?.setMeasurementDuration(20.0)

            sdk?.setMetricsBufferObserver { buffer ->
                runOnUiThread {
                    // ONLY move if we are actually waiting for data for the CURRENT question
                    if (isWaitingForData) {
                        Log.d("PresageTest", "Final Buffer Received for Question ${currentQuestion + 1}")

                        // Process your data here if needed..
                        if (buffer.hasPulse() && buffer.pulse.rateCount > 0) {
                            val pulse = buffer.pulse.getRate(buffer.pulse.rateCount - 1).value
//                            tvHeartRate.text = String.format("%.0f", pulse)
                            Log.i("PresageTest", "Final HR: $pulse")
                        }

                        // Final Respiration Rate from buffer
                        if (buffer.hasBreathing() && buffer.breathing.rateCount > 0) {
                            val resp = buffer.breathing.getRate(buffer.breathing.rateCount - 1).value
//                            tvRespRate.text = String.format("%.0f", resp)
                            Log.i("PresageTest", "Final Resp: $resp")
                        }

                        isWaitingForData = false // Unlock
                        moveToNextQuestion()
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("PresageTest", "Setup failed: ${e.javaClass.simpleName} - ${e.message}", e)
        }
    }

    private val presageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        Log.d("PresageTest", "Camera Activity Closed. Waiting for background buffer...")
    }

    private fun launchPresageCamera() {
        try {
            val intent = Intent()
            intent.setClassName(this, "com.presagetech.smartspectra.ui.SmartSpectraActivity")
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            presageLauncher.launch(intent)
        } catch (e: Exception) {
            isWaitingForData = false
            moveToNextQuestion()
        }
    }

    private fun moveToNextQuestion() {
        runOnUiThread {
            // Unlock the state immediately
            isWaitingForData = false

            if (currentQuestion < totalQuestions - 1) {
                currentQuestion++
                Log.d("PresageTest", "Incrementing to: $currentQuestion")

                displayQuestion()

                // HACK: Sometimes the camera activity gets "stuck" in front.
                // This ensures your ScreeningActivity is visible again.
                val intent = Intent(this, ScreeningActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)

            } else {
                finishScreening()
            }
        }
    }

    private fun displayQuestion() {
        tvQuestionCounter.text = "Question ${currentQuestion + 1} of $totalQuestions"
        tvQuestionText.text = questions[currentQuestion]
        selectedAnswer = -1
        radioButtons.forEach { it.isChecked = false }
        btnSubmit.isEnabled = false
        btnSubmit.alpha = 0.4f
        btnSubmit.text = "Record & Next" // Clear instruction for the user
    }

    private fun selectAnswer(score: Int) {
        selectedAnswer = score
        scores[currentQuestion] = score
        radioButtons.forEachIndexed { i, rb -> rb.isChecked = (i == score) }
        btnSubmit.isEnabled = true
        btnSubmit.alpha = 1.0f
    }

    private fun finishScreening() {
        val intent = Intent(this, BreatheActivity::class.java)
        intent.putExtra("total_score", scores.sum())
        startActivity(intent)
        finish()
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