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
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ScreeningActivity : AppCompatActivity() {

    private var currentQuestion = 0
    private var totalQuestions = 0
    private lateinit var scores: IntArray
    private lateinit var questions: Array<String>
    private lateinit var screeningTitle: String

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

        private val GAD7_QUESTIONS = arrayOf(
            "Over the last 2 weeks, how often have you been bothered by feeling nervous, anxious, or on edge?",
            "Over the last 2 weeks, how often have you been bothered by not being able to stop or control worrying?",
            "Over the last 2 weeks, how often have you been bothered by worrying too much about different things?",
            "Over the last 2 weeks, how often have you been bothered by trouble relaxing?",
            "Over the last 2 weeks, how often have you been bothered by being so restless that it is hard to sit still?",
            "Over the last 2 weeks, how often have you been bothered by becoming easily annoyed or irritable?",
            "Over the last 2 weeks, how often have you been bothered by feeling afraid, as if something awful might happen?"
        )
    }

    private lateinit var tvQuestionCounter: TextView
    private lateinit var tvQuestionLabel: TextView
    private lateinit var tvQuestionText: TextView
    private lateinit var progressFill: View
    private lateinit var answerCards: Array<LinearLayout>
    private lateinit var radioButtons: Array<RadioButton>
    private lateinit var btnSubmit: TextView
    private var selectedAnswer = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_screening)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load correct question set based on intent extra
        val screeningType = intent.getStringExtra(SelectScreeningActivity.EXTRA_SCREENING_TYPE)
            ?: SelectScreeningActivity.TYPE_PHQ9

        if (screeningType == SelectScreeningActivity.TYPE_GAD7) {
            questions = GAD7_QUESTIONS
            screeningTitle = getString(R.string.screening_title_gad7)
        } else {
            questions = PHQ9_QUESTIONS
            screeningTitle = getString(R.string.screening_title)
        }
        totalQuestions = questions.size
        scores = IntArray(totalQuestions)

        tvQuestionCounter = findViewById(R.id.tvQuestionCounter)
        tvQuestionLabel = findViewById(R.id.tvQuestionLabel)
        tvQuestionText = findViewById(R.id.tvQuestionText)
        progressFill = findViewById(R.id.screeningProgressFill)

        val tvScreeningTitle = findViewById<TextView>(R.id.tvScreeningTitle)
        tvScreeningTitle.text = screeningTitle

        answerCards = arrayOf(
            findViewById(R.id.answer0),
            findViewById(R.id.answer1),
            findViewById(R.id.answer2),
            findViewById(R.id.answer3)
        )

        radioButtons = arrayOf(
            findViewById(R.id.radio0),
            findViewById(R.id.radio1),
            findViewById(R.id.radio2),
            findViewById(R.id.radio3)
        )

        btnSubmit = findViewById(R.id.btnSubmitAnswer)

        val btnBack = findViewById<ImageView>(R.id.btnScreeningBack)
        btnBack.setOnClickListener { finish() }

        // Set up answer card click listeners
        for (i in answerCards.indices) {
            answerCards[i].setOnClickListener { selectAnswer(i) }
        }

        // Submit button
        btnSubmit.setOnClickListener { submitAnswer() }

        // Display initial question
        displayQuestion()

        // Start camera preview - TEMPORARILY DISABLED FOR DEBUGGING
        // if (checkCameraPermission()) {
        //     startCamera()
        // } else {
        //     requestCameraPermission()
        // }
    }

    private fun displayQuestion() {
        tvQuestionCounter.text = "Question ${currentQuestion + 1} of $totalQuestions"
        tvQuestionLabel.text = "QUESTION ${currentQuestion + 1}"
        tvQuestionText.text = questions[currentQuestion]

        // Update progress bar
        progressFill.post {
            val parent = progressFill.parent as View
            val params = progressFill.layoutParams
            params.width = (parent.width * ((currentQuestion + 1).toFloat() / totalQuestions)).toInt()
            progressFill.layoutParams = params
        }

        // Reset answer selection
        selectedAnswer = -1
        for (i in radioButtons.indices) {
            radioButtons[i].isChecked = false
            answerCards[i].setBackgroundResource(R.drawable.answer_card_background)
        }

        // Disable submit button
        btnSubmit.alpha = 0.4f
        btnSubmit.isEnabled = false
    }

    private fun selectAnswer(score: Int) {
        selectedAnswer = score

        // Update visual state
        for (i in radioButtons.indices) {
            radioButtons[i].isChecked = (i == score)
            answerCards[i].setBackgroundResource(
                if (i == score) R.drawable.answer_card_selected
                else R.drawable.answer_card_background
            )
        }

        // Store score
        scores[currentQuestion] = score

        // Enable submit button
        btnSubmit.alpha = 1.0f
        btnSubmit.isEnabled = true
    }

    private fun submitAnswer() {
        if (selectedAnswer < 0) return

        if (currentQuestion < totalQuestions - 1) {
            currentQuestion++
            displayQuestion()
        } else {
            finishScreening()
        }
    }

    private fun finishScreening() {
        val intent = Intent(this, BreatheActivity::class.java)
        intent.putExtra("total_score", scores.sum())
        intent.putExtra("screening_type",
            getIntent().getStringExtra(SelectScreeningActivity.EXTRA_SCREENING_TYPE) ?: SelectScreeningActivity.TYPE_PHQ9)
        startActivity(intent)
        finish()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                val previewView = findViewById<PreviewView>(R.id.cameraPreview)
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                Log.e("ScreeningActivity", "Camera bind failed", e)
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
            Toast.makeText(this, "Camera permission required for screening", Toast.LENGTH_LONG).show()
        }
    }
}
