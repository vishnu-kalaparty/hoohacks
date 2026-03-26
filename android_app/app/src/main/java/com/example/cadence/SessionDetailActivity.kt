package com.example.cadence

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cadence.api.PlaceholderData
import com.example.cadence.api.QuestionVital
import com.example.cadence.api.RetrofitClient
import com.example.cadence.api.SessionDetailResponse
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class SessionDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SessionDetail"
    }

    private var sessionId = -1
    private var patientName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_session_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionId = intent.getIntExtra("session_id", -1)
        patientName = intent.getStringExtra("patient_name") ?: "Patient"

        val date = intent.getStringExtra("checkin_date") ?: ""
        val scaleScore = intent.getIntExtra("scale_score", 0)
        val scaleType = intent.getStringExtra("scale_type") ?: "PHQ-9"
        val situationText = intent.getStringExtra("situation_text") ?: ""
        val copingText = intent.getStringExtra("coping_text") ?: ""
        val hrvValue = intent.getDoubleExtra("hrv_value", 0.0)
        val breathingRate = intent.getDoubleExtra("breathing_rate", 0.0)
        val distressRating = intent.getIntExtra("distress_rating", 0)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvDetailName).text = patientName
        findViewById<TextView>(R.id.tvDetailDate).text = formatDate(date)

        val maxScore = if (scaleType == "PHQ-9") 27 else 21
        findViewById<TextView>(R.id.tvDetailScoreBadge).text = "$scaleScore/$maxScore"
        findViewById<TextView>(R.id.tvDetailScoreSmall).text = "$scaleScore/$maxScore"
        findViewById<TextView>(R.id.tvQuestionsTitle).text = "$scaleType Responses"

        findViewById<TextView>(R.id.tvHrv).text = if (hrvValue > 0) "${hrvValue.toInt()}" else "–"
        findViewById<TextView>(R.id.tvBreathing).text = if (breathingRate > 0) String.format("%.1f", breathingRate) else "–"
        findViewById<TextView>(R.id.tvDistress).text = if (distressRating > 0) "$distressRating/10" else "–"

        findViewById<TextView>(R.id.tvSituation).text =
            if (situationText.isNotBlank()) situationText else "No situation recorded"
        findViewById<TextView>(R.id.tvCoping).text =
            if (copingText.isNotBlank()) copingText else "No coping strategies recorded"

        loadQuestionDetails()
    }

    private fun loadQuestionDetails() {
        if (sessionId <= 0) {
            displayQuestions(PlaceholderData.sessionDetail.questions)
            return
        }

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        RetrofitClient.api.getSessionDetail(sessionId).enqueue(object : Callback<SessionDetailResponse> {
            override fun onResponse(call: Call<SessionDetailResponse>, response: Response<SessionDetailResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    displayQuestions(body.questions)

                    body.session.let { s ->
                        if (s.situationText != null)
                            findViewById<TextView>(R.id.tvSituation).text = s.situationText
                        if (s.copingText != null)
                            findViewById<TextView>(R.id.tvCoping).text = s.copingText
                        if (s.hrvValue != null)
                            findViewById<TextView>(R.id.tvHrv).text = "${s.hrvValue.toInt()}"
                        if (s.breathingRate != null)
                            findViewById<TextView>(R.id.tvBreathing).text = String.format("%.1f", s.breathingRate)
                        if (s.distressRating != null)
                            findViewById<TextView>(R.id.tvDistress).text = "${s.distressRating}/10"
                    }
                } else {
                    Log.w(TAG, "API returned ${response.code()}, using passed-in data")
                    displayQuestions(PlaceholderData.sessionDetail.questions)
                }
            }

            override fun onFailure(call: Call<SessionDetailResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.w(TAG, "API call failed, using placeholder questions", t)
                displayQuestions(PlaceholderData.sessionDetail.questions)
            }
        })
    }

    private fun setupHrvChart(questions: List<QuestionVital>) {
        val chart = findViewById<BarChart>(R.id.hrvBarChart)
        val withHrv = questions.filter { it.hrvAtQuestion != null }
        if (withHrv.isEmpty()) {
            chart.visibility = View.GONE
            return
        }

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()

        val teal = Color.parseColor("#1E8B8B")
        val gray = Color.parseColor("#9CA3AF")
        val red = Color.parseColor("#DC2626")

        for ((i, q) in questions.withIndex()) {
            val hrv = q.hrvAtQuestion ?: 0.0
            entries.add(BarEntry(i.toFloat(), hrv.toFloat()))
            labels.add(q.questionId)

            val isLow = hrv < 35
            colors.add(
                when {
                    isLow -> red
                    q.isVitalsCorrelated == true -> teal
                    else -> gray
                }
            )
        }

        val dataSet = BarDataSet(entries, "HRV").apply {
            setColors(colors)
            valueTextSize = 9f
            valueTextColor = Color.parseColor("#374151")
        }

        chart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            animateY(500)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                textSize = 10f
                textColor = Color.parseColor("#6B7280")
                setDrawGridLines(false)
            }
            axisLeft.apply {
                textSize = 10f
                textColor = Color.parseColor("#6B7280")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E5E7EB")
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun setupScoreChart(questions: List<QuestionVital>) {
        val chart = findViewById<BarChart>(R.id.scoreBarChart)
        if (questions.isEmpty()) {
            chart.visibility = View.GONE
            return
        }

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()

        for ((i, q) in questions.withIndex()) {
            val score = (q.responseValue ?: 0).toFloat()
            entries.add(BarEntry(i.toFloat(), score))
            labels.add(q.questionId)

            colors.add(
                when (q.responseValue ?: 0) {
                    0 -> Color.parseColor("#D1FAE5")
                    1 -> Color.parseColor("#6EE7B7")
                    2 -> Color.parseColor("#F59E0B")
                    else -> Color.parseColor("#DC2626")
                }
            )
        }

        val dataSet = BarDataSet(entries, "Score").apply {
            setColors(colors)
            valueTextSize = 10f
            valueTextColor = Color.parseColor("#374151")
        }

        chart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setDrawGridBackground(false)
            animateY(500)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                textSize = 10f
                textColor = Color.parseColor("#6B7280")
                setDrawGridLines(false)
            }
            axisLeft.apply {
                textSize = 10f
                textColor = Color.parseColor("#6B7280")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E5E7EB")
                axisMinimum = 0f
                axisMaximum = 3.5f
                granularity = 1f
            }
            axisRight.isEnabled = false
            invalidate()
        }
    }

    private fun displayQuestions(questions: List<QuestionVital>) {
        setupHrvChart(questions)
        setupScoreChart(questions)

        val container = findViewById<LinearLayout>(R.id.questionsContainer)
        container.removeAllViews()

        val answerTexts = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")

        for (q in questions) {
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
                )
                setBackgroundColor(0xFFE5E7EB.toInt())
            }
            container.addView(divider)

            val block = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            block.addView(TextView(this).apply {
                text = "${q.questionId}: ${q.questionText ?: "Question"}"
                textSize = 14f
                setTextColor(Color.parseColor("#1A1A2E"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(6) }
            })

            val responseIdx = (q.responseValue ?: 0).coerceIn(0, 3)
            val answerLabel = "${answerTexts[responseIdx]} (${q.responseValue ?: 0})"

            block.addView(TextView(this).apply {
                text = answerLabel
                textSize = 12f
                setTextColor(Color.parseColor("#4B5563"))
                setBackgroundResource(R.drawable.answer_chip_background)
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(8) }
            })

            if (q.hrvAtQuestion != null) {
                val vitalsRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                vitalsRow.addView(TextView(this).apply {
                    text = "HRV: ${q.hrvAtQuestion.toInt()}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#6B7280"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = dpToPx(16) }
                })

                val isCorrelated = q.isVitalsCorrelated == true
                vitalsRow.addView(TextView(this).apply {
                    text = if (isCorrelated) "● Vitals correlated" else "○ Not correlated"
                    textSize = 11f
                    setTextColor(if (isCorrelated) Color.parseColor("#059669") else Color.parseColor("#9CA3AF"))
                    setTypeface(typeface, if (isCorrelated) Typeface.BOLD else Typeface.NORMAL)
                })

                block.addView(vitalsRow)
            }

            container.addView(block)
        }
    }

    private fun formatDate(raw: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outFmt = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            outFmt.format(inFmt.parse(raw.take(10))!!)
        } catch (_: Exception) {
            raw
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
