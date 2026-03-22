package com.example.cadence

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import com.example.cadence.api.PlaceholderData
import com.example.cadence.api.QuestionVital
import com.example.cadence.api.RetrofitClient
import com.example.cadence.api.SessionDetail
import com.example.cadence.api.SessionDetailResponse
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class SessionDetailActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvDateTitle: TextView
    private lateinit var tvScaleType: TextView
    private lateinit var tvScaleScore: TextView
    private lateinit var tvDistress: TextView
    private lateinit var tvHrv: TextView
    private lateinit var tvBreathing: TextView
    private lateinit var tvPulse: TextView
    private lateinit var tvSituation: TextView
    private lateinit var tvCoping: TextView
    private lateinit var llQuestions: LinearLayout

    private var sessionId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_detail)

        val main = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionId = intent.getIntExtra("session_id", 0)

        progressBar = findViewById(R.id.progressBar)
        tvDateTitle = findViewById(R.id.tvDateTitle)
        tvScaleType = findViewById(R.id.tvScaleType)
        tvScaleScore = findViewById(R.id.tvScaleScore)
        tvDistress = findViewById(R.id.tvDistress)
        tvHrv = findViewById(R.id.tvHrv)
        tvBreathing = findViewById(R.id.tvBreathing)
        tvPulse = findViewById(R.id.tvPulse)
        tvSituation = findViewById(R.id.tvSituation)
        tvCoping = findViewById(R.id.tvCoping)
        llQuestions = findViewById(R.id.llQuestions)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        loadSessionDetail()
    }

    private fun loadSessionDetail() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val response: SessionDetailResponse = try {
                RetrofitClient.api.getSessionDetail(sessionId)
            } catch (e: Exception) {
                Log.w("SessionDetail", "API unavailable, using placeholders", e)
                PlaceholderData.sessionDetail
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                bindSession(response.session)
                bindQuestions(response.questions)
            }
        }
    }

    private fun bindSession(s: SessionDetail) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFormat = SimpleDateFormat("EEEE, MMM dd yyyy", Locale.US)
        tvDateTitle.text = try {
            val date = dateFormat.parse(s.checkinDate.take(10))
            displayFormat.format(date!!)
        } catch (_: Exception) {
            s.checkinDate
        }

        tvScaleType.text = s.scaleType
        tvScaleScore.text = s.scaleScore.toString()
        tvDistress.text = "Distress: ${s.distressRating ?: "--"}/10"
        tvHrv.text = s.hrvValue?.let { "HRV: %.1f ms".format(it) } ?: "HRV: --"
        tvBreathing.text = s.breathingRate?.let { "Breathing: %.1f/min".format(it) } ?: "Breathing: --"
        tvPulse.text = s.pulseRate?.let { "Pulse: %.0f bpm".format(it) } ?: "Pulse: --"
        tvSituation.text = s.situationText ?: "No situation recorded"
        tvCoping.text = s.copingText ?: "No coping strategy recorded"
    }

    private fun bindQuestions(questions: List<QuestionVital>) {
        llQuestions.removeAllViews()

        if (questions.isEmpty()) {
            findViewById<TextView>(R.id.tvQuestionsHeader).visibility = View.GONE
            return
        }

        for (q in questions) {
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8.dpToPx() }
                radius = 12f.dpToPxF()
                cardElevation = 2f.dpToPxF()
                setCardBackgroundColor(Color.parseColor("#CC1E8B8B"))
                strokeWidth = 0
            }

            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
            }

            val tvQuestion = TextView(this).apply {
                text = "${q.questionId}: ${q.questionText}"
                setTextColor(Color.WHITE)
                textSize = 14f
            }

            val tvResponse = TextView(this).apply {
                text = buildString {
                    append("Response: ${q.responseValue}")
                    q.hrvAtQuestion?.let { append("  |  HRV: %.1f".format(it)) }
                }
                setTextColor(Color.parseColor("#E0F2F7"))
                textSize = 12f
            }

            inner.addView(tvQuestion)
            inner.addView(tvResponse)
            card.addView(inner)
            llQuestions.addView(card)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Float.dpToPxF(): Float = this * resources.displayMetrics.density
}
