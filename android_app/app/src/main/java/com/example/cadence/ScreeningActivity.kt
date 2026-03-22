package com.example.cadence

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.presagetech.smartspectra.SmartSpectraSdk

class ScreeningActivity : AppCompatActivity() {

    private var currentQuestion = 0
    private var totalQuestions = 0
    private lateinit var scores: IntArray
    private lateinit var hrvPerQuestion: DoubleArray
    private lateinit var questions: Array<String>
    private lateinit var questionIds: Array<String>
    private var screeningType: String = SelectScreeningActivity.TYPE_PHQ9

    private lateinit var tvQuestionCounter: TextView
    private lateinit var tvQuestionText: TextView
    private lateinit var progressFill: View
    private lateinit var answerCards: Array<LinearLayout>
    private lateinit var radioButtons: Array<RadioButton>
    private lateinit var btnSubmit: TextView
    private var selectedAnswer = -1

    private var sdk: SmartSpectraSdk? = null
    private var isWaitingForData = false

    private var lastPulseRate = 0.0
    private var lastBreathingRate = 0.0
    private var totalPulse = 0.0
    private var totalBreathing = 0.0
    private var vitalsSampleCount = 0

    companion object {
        private const val TAG = "ScreeningActivity"

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

        private val GAD7_QUESTIONS = arrayOf(
            "Over the last 2 weeks, how often have you been bothered by feeling nervous, anxious, or on edge?",
            "Over the last 2 weeks, how often have you been bothered by not being able to stop or control worrying?",
            "Over the last 2 weeks, how often have you been bothered by worrying too much about different things?",
            "Over the last 2 weeks, how often have you been bothered by trouble relaxing?",
            "Over the last 2 weeks, how often have you been bothered by being so restless that it's hard to sit still?",
            "Over the last 2 weeks, how often have you been bothered by becoming easily annoyed or irritable?",
            "Over the last 2 weeks, how often have you been bothered by feeling afraid, as if something awful might happen?"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screening)

        screeningType = intent.getStringExtra(SelectScreeningActivity.EXTRA_SCREENING_TYPE)
            ?: SelectScreeningActivity.TYPE_PHQ9

        setupUI()
        setupPresageSDK()
        displayQuestion()
    }

    private fun setupUI() {
        val isPHQ9 = screeningType == SelectScreeningActivity.TYPE_PHQ9
        questions = if (isPHQ9) PHQ9_QUESTIONS else GAD7_QUESTIONS
        totalQuestions = questions.size
        scores = IntArray(totalQuestions)
        hrvPerQuestion = DoubleArray(totalQuestions)

        questionIds = Array(totalQuestions) { "Q${it + 1}" }

        tvQuestionCounter = findViewById(R.id.tvQuestionCounter)
        tvQuestionText = findViewById(R.id.tvQuestionText)
        progressFill = findViewById(R.id.screeningProgressFill)
        btnSubmit = findViewById(R.id.btnSubmitAnswer)

        answerCards = arrayOf(
            findViewById(R.id.answer0), findViewById(R.id.answer1),
            findViewById(R.id.answer2), findViewById(R.id.answer3)
        )
        radioButtons = arrayOf(
            findViewById(R.id.radio0), findViewById(R.id.radio1),
            findViewById(R.id.radio2), findViewById(R.id.radio3)
        )

        for (i in answerCards.indices) {
            answerCards[i].setOnClickListener { selectAnswer(i) }
        }

        btnSubmit.setOnClickListener {
            if (selectedAnswer < 0) return@setOnClickListener
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
                    if (isWaitingForData) {
                        Log.d(TAG, "Buffer received for Q${currentQuestion + 1}")

                        if (buffer.hasPulse() && buffer.pulse.rateCount > 0) {
                            val pulse = buffer.pulse.getRate(buffer.pulse.rateCount - 1).value.toDouble()
                            lastPulseRate = pulse
                            totalPulse += pulse
                            hrvPerQuestion[currentQuestion] = pulse
                            Log.i(TAG, "HR for Q${currentQuestion + 1}: $pulse")
                        }

                        if (buffer.hasBreathing() && buffer.breathing.rateCount > 0) {
                            val resp = buffer.breathing.getRate(buffer.breathing.rateCount - 1).value.toDouble()
                            lastBreathingRate = resp
                            totalBreathing += resp
                            Log.i(TAG, "RR for Q${currentQuestion + 1}: $resp")
                        }

                        vitalsSampleCount++
                        isWaitingForData = false
                        moveToNextQuestion()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Presage setup failed", e)
        }
    }

    private val presageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        Log.d(TAG, "Camera closed, waiting for buffer...")
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
            isWaitingForData = false
            if (currentQuestion < totalQuestions - 1) {
                currentQuestion++
                displayQuestion()
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
        btnSubmit.text = "Record & Next"
    }

    private fun selectAnswer(score: Int) {
        selectedAnswer = score
        scores[currentQuestion] = score
        radioButtons.forEachIndexed { i, rb -> rb.isChecked = (i == score) }
        btnSubmit.isEnabled = true
        btnSubmit.alpha = 1.0f
    }

    private fun finishScreening() {
        val avgHrv = if (vitalsSampleCount > 0) totalPulse / vitalsSampleCount else 0.0
        val avgBreathing = if (vitalsSampleCount > 0) totalBreathing / vitalsSampleCount else 0.0

        val intent = Intent(this, BreatheActivity::class.java).apply {
            putExtra("total_score", scores.sum())
            putExtra("screening_type", screeningType)
            putExtra("scores_array", scores)
            putExtra("hrv_array", hrvPerQuestion)
            putExtra("question_ids", questionIds)
            putExtra("avg_hrv", avgHrv)
            putExtra("avg_breathing", avgBreathing)
            putExtra("last_pulse", lastPulseRate)
        }
        startActivity(intent)
        finish()
    }

    private fun checkPermissions() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

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
