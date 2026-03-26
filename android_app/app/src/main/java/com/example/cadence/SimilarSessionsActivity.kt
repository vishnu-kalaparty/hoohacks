package com.example.cadence

import android.content.Intent
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
import com.example.cadence.api.RetrofitClient
import com.example.cadence.api.SimilarSession
import com.example.cadence.api.SimilarSessionsResponse
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class SimilarSessionsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SimilarSessions"
    }

    private lateinit var lineChart: LineChart
    private lateinit var sessionsContainer: LinearLayout
    private lateinit var tvSessionCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    private var patientName = ""
    private var patientId = -1
    private var sessions: List<SimilarSession> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_similar_sessions)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        patientName = intent.getStringExtra("patient_name") ?: "Patient"
        patientId = intent.getIntExtra("patient_id", -1)
        val severity = intent.getStringExtra("severity") ?: "Moderate"
        val date = intent.getStringExtra("patient_date") ?: ""

        findViewById<TextView>(R.id.tvHeaderName).text = patientName
        findViewById<TextView>(R.id.tvHeaderDate).text = date
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val tvBadge = findViewById<TextView>(R.id.tvHeaderBadge)
        tvBadge.text = severity
        styleBadge(tvBadge, severity)

        lineChart = findViewById(R.id.lineChart)
        sessionsContainer = findViewById(R.id.sessionsContainer)
        tvSessionCount = findViewById(R.id.tvSessionCount)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        loadSimilarSessions()
    }

    private fun loadSimilarSessions() {
        if (patientId <= 0) {
            showPlaceholder("Using sample data (no patient ID)")
            return
        }

        progressBar.visibility = View.VISIBLE

        RetrofitClient.api.getSimilarSessions(patientId).enqueue(object : Callback<SimilarSessionsResponse> {
            override fun onResponse(call: Call<SimilarSessionsResponse>, response: Response<SimilarSessionsResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    sessions = body.similarSessions
                    displayData(sessions)
                } else {
                    Log.w(TAG, "API returned ${response.code()}, falling back to placeholders")
                    showPlaceholder("Using sample data (server returned ${response.code()})")
                }
            }

            override fun onFailure(call: Call<SimilarSessionsResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Log.w(TAG, "API call failed, using placeholders", t)
                showPlaceholder("Using sample data (offline mode)")
            }
        })
    }

    private fun showPlaceholder(message: String) {
        tvStatus.text = message
        tvStatus.visibility = View.VISIBLE
        sessions = PlaceholderData.similarSessions
        displayData(sessions)
    }

    private fun displayData(sessionList: List<SimilarSession>) {
        tvSessionCount.text = "${sessionList.size} found"
        setupChart(sessionList)
        populateSessionCards(sessionList)
    }

    // ── Chart ─────────────────────────────────────────────────────────────

    private fun setupChart(sessionList: List<SimilarSession>) {
        val sorted = sessionList.sortedBy { it.checkinDate }
        if (sorted.isEmpty()) return

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFormat = SimpleDateFormat("MMM dd", Locale.US)

        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()

        for ((i, s) in sorted.withIndex()) {
            entries.add(Entry(i.toFloat(), (s.scaleScore ?: 0).toFloat()))
            val label = try {
                displayFormat.format(inputFormat.parse(s.checkinDate.take(10))!!)
            } catch (_: Exception) {
                s.checkinDate.take(10)
            }
            labels.add(label)
        }

        val dataSet = LineDataSet(entries, "Scale Score").apply {
            color = Color.parseColor("#1E8B8B")
            lineWidth = 2.5f
            setCircleColor(Color.parseColor("#1E8B8B"))
            circleRadius = 5f
            circleHoleRadius = 2.5f
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.parseColor("#374151")
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#1E8B8B")
            fillAlpha = 25
            highLightColor = Color.parseColor("#EA580C")
            highlightLineWidth = 1.5f
        }

        lineChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            animateX(600)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                textSize = 10f
                textColor = Color.parseColor("#6B7280")
                setDrawGridLines(false)
                labelRotationAngle = -30f
            }
            axisLeft.apply {
                textSize = 11f
                textColor = Color.parseColor("#6B7280")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E5E7EB")
                axisMinimum = 0f
            }
            axisRight.isEnabled = false

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    if (e != null) {
                        val idx = e.x.toInt()
                        if (idx in sorted.indices) {
                            openSessionDetail(sorted[idx])
                        }
                    }
                }
                override fun onNothingSelected() {}
            })

            invalidate()
        }
    }

    // ── Session cards ─────────────────────────────────────────────────────

    private fun populateSessionCards(sessionList: List<SimilarSession>) {
        sessionsContainer.removeAllViews()
        val sorted = sessionList.sortedByDescending { it.similarity ?: 0.0 }

        for (session in sorted) {
            val card = buildSessionCard(session)
            sessionsContainer.addView(card)
        }
    }

    private fun buildSessionCard(session: SimilarSession): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.chart_card_background)
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            elevation = dpToPx(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(10) }
            isClickable = true
            isFocusable = true
            setOnClickListener { openSessionDetail(session) }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(8) }
        }

        val dateText = formatDate(session.checkinDate)
        topRow.addView(TextView(this).apply {
            text = dateText
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1A2E"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val scoreText = "Score: ${session.scaleScore ?: "\u2013"}"
        topRow.addView(TextView(this).apply {
            text = scoreText
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getScoreColor(session.scaleScore))
        })

        card.addView(topRow)

        val simPercent = ((session.similarity ?: 0.0) * 100).toInt()
        val metaRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dpToPx(6) }
        }

        metaRow.addView(TextView(this).apply {
            text = "${simPercent}% similar"
            textSize = 12f
            setTextColor(Color.parseColor("#1E8B8B"))
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dpToPx(16) }
        })

        if (session.hrvValue != null) {
            metaRow.addView(TextView(this).apply {
                text = "HRV: ${session.hrvValue.toInt()}"
                textSize = 12f
                setTextColor(Color.parseColor("#6B7280"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dpToPx(16) }
            })
        }

        if (session.distressRating != null) {
            metaRow.addView(TextView(this).apply {
                text = "Distress: ${session.distressRating}/10"
                textSize = 12f
                setTextColor(Color.parseColor("#6B7280"))
            })
        }

        card.addView(metaRow)

        if (!session.situationText.isNullOrBlank()) {
            val situationPreview = if (session.situationText.length > 80)
                session.situationText.take(80) + "\u2026" else session.situationText
            card.addView(TextView(this).apply {
                text = situationPreview
                textSize = 12f
                setTextColor(Color.parseColor("#4B5563"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(6) }
            })
        }

        card.addView(TextView(this).apply {
            text = "Tap to view full session \u2192"
            textSize = 11f
            setTextColor(Color.parseColor("#1E8B8B"))
        })

        return card
    }

    // ── Navigation ────────────────────────────────────────────────────────

    private fun openSessionDetail(session: SimilarSession) {
        val intent = Intent(this, SessionDetailActivity::class.java).apply {
            putExtra("session_id", session.sessionId)
            putExtra("patient_name", patientName)
            putExtra("patient_id", patientId)
            putExtra("checkin_date", session.checkinDate)
            putExtra("scale_score", session.scaleScore ?: 0)
            putExtra("scale_type", session.scaleType ?: "PHQ-9")
            putExtra("situation_text", session.situationText ?: "")
            putExtra("coping_text", session.copingText ?: "")
            putExtra("hrv_value", session.hrvValue ?: 0.0)
            putExtra("breathing_rate", session.breathingRate ?: 0.0)
            putExtra("distress_rating", session.distressRating ?: 0)
        }
        startActivity(intent)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun formatDate(raw: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            outputFormat.format(inputFormat.parse(raw.take(10))!!)
        } catch (_: Exception) {
            raw
        }
    }

    private fun getScoreColor(score: Int?): Int {
        return when {
            score == null -> Color.parseColor("#6B7280")
            score >= 20 -> Color.parseColor("#DC2626")
            score >= 15 -> Color.parseColor("#EA580C")
            score >= 10 -> Color.parseColor("#059669")
            else -> Color.parseColor("#0284C7")
        }
    }

    private fun styleBadge(tv: TextView, severity: String) {
        when (severity) {
            "Critical" -> {
                tv.setBackgroundResource(R.drawable.badge_critical)
                tv.setTextColor(0xFFDC2626.toInt())
            }
            "Severe" -> {
                tv.setBackgroundResource(R.drawable.badge_severe)
                tv.setTextColor(0xFFEA580C.toInt())
            }
            "Moderate" -> {
                tv.setBackgroundResource(R.drawable.badge_moderate)
                tv.setTextColor(0xFF059669.toInt())
            }
            else -> {
                tv.setBackgroundResource(R.drawable.badge_mild)
                tv.setTextColor(0xFF0284C7.toInt())
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
