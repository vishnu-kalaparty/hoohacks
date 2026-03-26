package com.example.cadence

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PatientAnalyticsActivity : AppCompatActivity() {

    data class QuestionResult(
        val questionNumber: Int,
        val questionText: String,
        val answerText: String,
        val answerScore: Int,
        val heartRate: Int,
        val respRate: Int,
        val isDiscrepancy: Boolean,
        val discrepancyNote: String = ""
    )

    private lateinit var vitalsChart: VitalsChartView
    private lateinit var tabHeartRate: TextView
    private lateinit var tabRespRate: TextView
    private lateinit var tvNormalRange: TextView
    private var showingHeartRate = true

    private var questionResults: List<QuestionResult> = emptyList()

    // Stored for PDF export and notes
    private var patientName = ""
    private var patientDate = ""
    private var screeningType = ""
    private var totalScore = 0
    private var maxScore = 27
    private var severityLabel = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient_analytics)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        patientName = intent.getStringExtra("patient_name") ?: "Patient"
        patientDate = intent.getStringExtra("patient_date") ?: ""
        screeningType = intent.getStringExtra("screening_type") ?: "PHQ-9"
        val score = intent.getIntExtra("score", 0)
        totalScore = score
        val severity = intent.getStringExtra("severity") ?: "Moderate"

        // Header
        findViewById<TextView>(R.id.tvHeaderName).text = patientName
        findViewById<TextView>(R.id.tvHeaderDate).text = patientDate
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Severity badge styling
        val tvBadge = findViewById<TextView>(R.id.tvHeaderBadge)
        tvBadge.text = severity
        when (severity) {
            "Critical" -> {
                tvBadge.setBackgroundResource(R.drawable.badge_critical)
                tvBadge.setTextColor(0xFFDC2626.toInt())
            }
            "Severe" -> {
                tvBadge.setBackgroundResource(R.drawable.badge_severe)
                tvBadge.setTextColor(0xFFEA580C.toInt())
            }
            "Moderate" -> {
                tvBadge.setBackgroundResource(R.drawable.badge_moderate)
                tvBadge.setTextColor(0xFF059669.toInt())
            }
            else -> {
                tvBadge.setBackgroundResource(R.drawable.badge_mild)
                tvBadge.setTextColor(0xFF0284C7.toInt())
            }
        }

        // Generate sample question results based on screening type
        questionResults = generateSampleData(screeningType, score)

        // Chart setup
        vitalsChart = findViewById(R.id.vitalsChart)
        tabHeartRate = findViewById(R.id.tabHeartRate)
        tabRespRate = findViewById(R.id.tabRespRate)
        tvNormalRange = findViewById(R.id.tvNormalRange)

        tabHeartRate.setOnClickListener { switchTab(true) }
        tabRespRate.setOnClickListener { switchTab(false) }
        updateChart()

        // Screening section title and score
        val tvScreeningTitle = findViewById<TextView>(R.id.tvScreeningTitle)
        val tvScoreBadge = findViewById<TextView>(R.id.tvScoreBadge)
        maxScore = if (screeningType == "PHQ-9") 27 else 21
        tvScreeningTitle.text = if (screeningType == "PHQ-9")
            getString(R.string.analytics_phq9_title) else getString(R.string.analytics_gad7_title)
        tvScoreBadge.text = "$score/$maxScore"

        // Score Summary Card
        val screenLabel = if (screeningType == "PHQ-9") "PHQ-9 (Depression)" else "GAD-7 (Anxiety)"
        findViewById<TextView>(R.id.tvSummaryType).text = screenLabel
        findViewById<TextView>(R.id.tvSummaryScore).text = "$score / $maxScore"

        val tvSummarySeverity = findViewById<TextView>(R.id.tvSummarySeverity)
        severityLabel = getSeverityLabel(screeningType, score)
        val severityText = severityLabel
        tvSummarySeverity.text = severityText
        when (severityText) {
            "Critical" -> {
                tvSummarySeverity.setBackgroundResource(R.drawable.badge_critical)
                tvSummarySeverity.setTextColor(0xFFDC2626.toInt())
            }
            "Severe" -> {
                tvSummarySeverity.setBackgroundResource(R.drawable.badge_severe)
                tvSummarySeverity.setTextColor(0xFFEA580C.toInt())
            }
            "Moderate" -> {
                tvSummarySeverity.setBackgroundResource(R.drawable.badge_moderate)
                tvSummarySeverity.setTextColor(0xFF059669.toInt())
            }
            "Mild" -> {
                tvSummarySeverity.setBackgroundResource(R.drawable.badge_mild)
                tvSummarySeverity.setTextColor(0xFF0284C7.toInt())
            }
            else -> {
                tvSummarySeverity.setBackgroundResource(R.drawable.badge_mild)
                tvSummarySeverity.setTextColor(0xFF0284C7.toInt())
            }
        }

        // Simulated previous score (slightly lower for demo)
        val prevScore = (score * 0.7).toInt().coerceAtLeast(0)
        val prevSeverity = getSeverityLabel(screeningType, prevScore)
        findViewById<TextView>(R.id.tvSummaryPrev).text = "Prev: $prevScore ($prevSeverity)"

        val tvTrend = findViewById<TextView>(R.id.tvSummaryTrend)
        if (score > prevScore) {
            tvTrend.text = "↑ Worsening"
            tvTrend.setTextColor(0xFFEA580C.toInt())
        } else if (score < prevScore) {
            tvTrend.text = "↓ Improving"
            tvTrend.setTextColor(0xFF059669.toInt())
        } else {
            tvTrend.text = "→ Stable"
            tvTrend.setTextColor(0xFF6B7280.toInt())
        }

        // Discrepancy alert banner
        val discrepancies = questionResults.filter { it.isDiscrepancy }
        val alertBanner = findViewById<LinearLayout>(R.id.alertBanner)
        if (discrepancies.isNotEmpty()) {
            alertBanner.visibility = View.VISIBLE
            val first = discrepancies.first()
            findViewById<TextView>(R.id.tvAlertTitle).text =
                "${discrepancies.size} DISCREPANCY DETECTED"
            findViewById<TextView>(R.id.tvAlertBody).text =
                "$screeningType Q${first.questionNumber}: Answer inconsistent with vitals"
        }

        // Populate questions
        populateQuestions()

        // Populate AI-Generated Clinical Insights
        populateInsights(screeningType, score, maxScore, discrepancies)

        // Button handlers
        findViewById<View>(R.id.btnExportPdf).setOnClickListener {
            exportPdf()
        }
        findViewById<View>(R.id.btnAddNotes).setOnClickListener {
            showSessionNotesDialog()
        }
        findViewById<View>(R.id.btnSimilarSessions).setOnClickListener {
            val ssIntent = Intent(this, SimilarSessionsActivity::class.java)
            ssIntent.putExtra("patient_name", patientName)
            ssIntent.putExtra("patient_id", getIntent().getIntExtra("patient_id", -1))
            ssIntent.putExtra("patient_date", patientDate)
            ssIntent.putExtra("severity", severityLabel)
            startActivity(ssIntent)
        }
    }

    private fun switchTab(heartRate: Boolean) {
        showingHeartRate = heartRate
        if (heartRate) {
            tabHeartRate.setTextColor(getColor(R.color.icon_teal))
            tabHeartRate.setBackgroundResource(R.drawable.tab_active)
            tabRespRate.setTextColor(getColor(R.color.text_gray))
            tabRespRate.background = null
            tvNormalRange.text = getString(R.string.analytics_normal_hr)
        } else {
            tabRespRate.setTextColor(getColor(R.color.icon_teal))
            tabRespRate.setBackgroundResource(R.drawable.tab_active)
            tabHeartRate.setTextColor(getColor(R.color.text_gray))
            tabHeartRate.background = null
            tvNormalRange.text = getString(R.string.analytics_normal_rr)
        }
        updateChart()
    }

    private fun updateChart() {
        // Only chart the "high-signal" questions (score >= 2 or discrepancy)
        val highSignal = questionResults.filter { it.answerScore >= 2 || it.isDiscrepancy }
        val displayData = if (highSignal.isEmpty()) questionResults.take(5) else highSignal

        if (showingHeartRate) {
            val points = displayData.map { it.heartRate.toFloat() }
            val labels = displayData.map { "Q${it.questionNumber}" }
            val highlightIdx = displayData.indexOfFirst { it.isDiscrepancy }
            vitalsChart.setData(points, labels, highlightIdx, 50f, 110f, 60f, 100f)
        } else {
            val points = displayData.map { it.respRate.toFloat() }
            val labels = displayData.map { "Q${it.questionNumber}" }
            val highlightIdx = displayData.indexOfFirst { it.isDiscrepancy }
            vitalsChart.setData(points, labels, highlightIdx, 8f, 32f, 12f, 20f)
        }
    }

    private fun populateQuestions() {
        val container = findViewById<LinearLayout>(R.id.questionsContainer)
        container.removeAllViews()

        for (qr in questionResults) {
            // Divider
            val divider = View(this)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
            )
            divider.setBackgroundColor(0xFFE5E7EB.toInt())
            container.addView(divider)

            // Question block
            val questionBlock = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            }

            // Question text
            val tvQuestion = TextView(this).apply {
                text = "Q${qr.questionNumber}: ${qr.questionText}"
                textSize = 14f
                setTextColor(0xFF1A1A2E.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(8) }
            }
            questionBlock.addView(tvQuestion)

            // Answer chip
            val answerChip = TextView(this).apply {
                text = "${qr.answerText} (${qr.answerScore})"
                textSize = 12f
                setTextColor(0xFF4B5563.toInt())
                setBackgroundResource(R.drawable.answer_chip_background)
                setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(12) }
            }
            questionBlock.addView(answerChip)

            // Show vitals for high-signal questions (score >= 2) or discrepancies
            if (qr.isDiscrepancy || qr.answerScore >= 2) {
                // Vitals row: Heart Rate | Respiratory Rate side by side
                val vitalsRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpToPx(8) }
                }

                // Heart Rate column
                val hrCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                }
                hrCol.addView(TextView(this).apply {
                    text = "Heart Rate"
                    textSize = 11f
                    setTextColor(0xFF9CA3AF.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpToPx(2) }
                })
                hrCol.addView(TextView(this).apply {
                    text = "${qr.heartRate} bpm"
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(if (qr.isDiscrepancy) 0xFFDC2626.toInt() else 0xFF1A1A2E.toInt())
                })
                vitalsRow.addView(hrCol)

                // Respiratory Rate column
                val rrCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                }
                rrCol.addView(TextView(this).apply {
                    text = "Respiratory Rate"
                    textSize = 11f
                    setTextColor(0xFF9CA3AF.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpToPx(2) }
                })
                rrCol.addView(TextView(this).apply {
                    text = "${qr.respRate} br/min"
                    textSize = 16f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(if (qr.isDiscrepancy) 0xFFDC2626.toInt() else 0xFF1A1A2E.toInt())
                })
                vitalsRow.addView(rrCol)

                if (qr.isDiscrepancy) {
                    // Red discrepancy card wrapping vitals + warning
                    val discCard = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(R.drawable.alert_discrepancy_red_background)
                        setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    discCard.addView(vitalsRow)

                    // Warning header
                    val discHeader = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = dpToPx(6) }
                    }
                    val warnIcon = ImageView(this@PatientAnalyticsActivity).apply {
                        setImageResource(R.drawable.ic_warning_red)
                        layoutParams = LinearLayout.LayoutParams(dpToPx(18), dpToPx(18)).apply {
                            marginEnd = dpToPx(6)
                        }
                    }
                    discHeader.addView(warnIcon)
                    discHeader.addView(TextView(this@PatientAnalyticsActivity).apply {
                        text = getString(R.string.analytics_discrepancy)
                        textSize = 12f
                        setTextColor(0xFFDC2626.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    discCard.addView(discHeader)

                    // Discrepancy body text
                    discCard.addView(TextView(this@PatientAnalyticsActivity).apply {
                        text = qr.discrepancyNote
                        textSize = 12f
                        setTextColor(0xFF991B1B.toInt())
                    })

                    questionBlock.addView(discCard)
                } else {
                    // Normal vitals display (not in a colored card)
                    questionBlock.addView(vitalsRow)

                    // Green "Vitals consistent" label
                    val consistentRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                    consistentRow.addView(ImageView(this).apply {
                        setImageResource(R.drawable.ic_check_circle_green)
                        layoutParams = LinearLayout.LayoutParams(dpToPx(16), dpToPx(16)).apply {
                            marginEnd = dpToPx(5)
                        }
                    })
                    consistentRow.addView(TextView(this).apply {
                        text = getString(R.string.analytics_vitals_consistent)
                        textSize = 12f
                        setTextColor(0xFF059669.toInt())
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    questionBlock.addView(consistentRow)
                }
            }

            container.addView(questionBlock)
        }
    }

    private fun populateInsights(
        screeningType: String,
        score: Int,
        maxScore: Int,
        discrepancies: List<QuestionResult>
    ) {
        val keyFindingsContainer = findViewById<LinearLayout>(R.id.keyFindingsContainer)
        val discussionPointsContainer = findViewById<LinearLayout>(R.id.discussionPointsContainer)

        // Generate key findings based on actual data
        val findings = mutableListOf<String>()
        val severity = getSeverityLabel(screeningType, score)
        val screenLabel = if (screeningType == "PHQ-9") "Depression" else "Anxiety"

        // Finding 1: Score summary
        findings.add("$screenLabel score is $score/$maxScore, placing patient in the $severity range.")

        // Finding 2: Discrepancy if present
        if (discrepancies.isNotEmpty()) {
            val dq = discrepancies.first()
            val boldPrefix = "Critical discrepancy detected: "
            val detail = if (screeningType == "PHQ-9")
                "Patient denied suicidal ideation while showing strongest physiological stress response."
            else
                "Patient denied severe fear while showing strongest physiological stress response."
            findings.add("$boldPrefix$detail")
        }

        // Finding 3: High-scoring questions
        val highQ = questionResults.filter { it.answerScore >= 2 }
        if (highQ.isNotEmpty()) {
            findings.add("${highQ.size} of ${questionResults.size} questions scored ≥2, indicating persistent symptoms in multiple areas.")
        }

        // Finding 4: Vitals consistency summary
        val consistentCount = questionResults.count { !it.isDiscrepancy && it.answerScore >= 1 }
        if (consistentCount > 0) {
            findings.add("Physiological responses generally consistent with self-reported symptoms${if (discrepancies.isNotEmpty()) " except Q${discrepancies.first().questionNumber}" else ""}.")
        }

        // Render key findings
        for (i in findings.indices) {
            val findingView = TextView(this).apply {
                textSize = 13f
                setTextColor(0xFF374151.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(10) }
            }

            val numbered = "${i + 1}. ${findings[i]}"
            // Bold the part before the colon on finding 2 (discrepancy)
            if (i == 1 && discrepancies.isNotEmpty()) {
                val boldEnd = numbered.indexOf(": ") + 2
                if (boldEnd > 2) {
                    val spannable = SpannableString(numbered)
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        0, boldEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    findingView.text = spannable
                } else {
                    findingView.text = numbered
                }
            } else {
                findingView.text = numbered
            }

            keyFindingsContainer.addView(findingView)
        }

        // Generate discussion points
        data class DiscussionPoint(
            val priority: Int, // 1=red, 2=orange, 3=green
            val title: String,
            val body: String
        )

        val points = mutableListOf<DiscussionPoint>()

        // Priority 1: Safety if discrepancy on sensitive question
        if (discrepancies.isNotEmpty() && screeningType == "PHQ-9") {
            points.add(
                DiscussionPoint(
                    1,
                    "Priority 1 - Safety Assessment",
                    "Explore suicidal ideation despite denial; physiological response suggests distress"
                )
            )
        }

        // Priority 2: Core symptom management
        if (screeningType == "PHQ-9") {
            points.add(
                DiscussionPoint(
                    2,
                    "Priority ${points.size + 1} - Depression Management",
                    "Address ${if (score >= 15) "worsening" else "current"} depression symptoms and review current treatment plan"
                )
            )
        } else {
            points.add(
                DiscussionPoint(
                    2,
                    "Priority ${points.size + 1} - Anxiety Management",
                    "Address ${if (score >= 10) "elevated" else "current"} anxiety symptoms and explore coping strategies"
                )
            )
        }

        // Priority 3: Positive progress or monitoring
        if (score < maxScore / 2) {
            points.add(
                DiscussionPoint(
                    3,
                    "Priority ${points.size + 1} - Positive Progress",
                    "Acknowledge symptom levels in ${severity.lowercase()} range and reinforce management strategies"
                )
            )
        } else {
            points.add(
                DiscussionPoint(
                    3,
                    "Priority ${points.size + 1} - Treatment Adjustment",
                    "Consider adjustments to treatment plan given $severity symptom levels"
                )
            )
        }

        // Render discussion points
        for (point in points) {
            val pointLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(14) }
            }

            // Priority dot
            val dotRes = when (point.priority) {
                1 -> R.drawable.priority_dot_red
                2 -> R.drawable.priority_dot_orange
                else -> R.drawable.priority_dot_green
            }
            val dot = ImageView(this).apply {
                setImageResource(dotRes)
                layoutParams = LinearLayout.LayoutParams(dpToPx(10), dpToPx(10)).apply {
                    marginEnd = dpToPx(10)
                    topMargin = dpToPx(4)
                }
            }
            pointLayout.addView(dot)

            // Text column
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }

            val titleView = TextView(this).apply {
                text = point.title
                textSize = 13f
                setTextColor(0xFF1A1A2E.toInt())
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(2) }
            }
            textCol.addView(titleView)

            val bodyView = TextView(this).apply {
                text = point.body
                textSize = 12f
                setTextColor(0xFF6B7280.toInt())
            }
            textCol.addView(bodyView)

            pointLayout.addView(textCol)
            discussionPointsContainer.addView(pointLayout)
        }
    }

    // ── Export PDF ───────────────────────────────────────────────────────
    private fun exportPdf() {
        val document = PdfDocument()
        val pageWidth = 595  // A4 width in points
        val pageHeight = 842 // A4 height in points

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1E8B8B")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1A1A2E")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 12f
            isAntiAlias = true
        }
        val mutedPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 11f
            isAntiAlias = true
        }
        val boldBodyPaint = Paint().apply {
            color = Color.parseColor("#1A1A2E")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val redPaint = Paint().apply {
            color = Color.parseColor("#DC2626")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val greenPaint = Paint().apply {
            color = Color.parseColor("#059669")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }

        val marginLeft = 40f
        val marginRight = pageWidth - 40f
        var y = 50f
        var pageNum = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        fun checkNewPage(needed: Float): Canvas {
            if (y + needed > pageHeight - 50f) {
                document.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = document.startPage(pageInfo)
                y = 50f
                return page.canvas
            }
            return canvas
        }

        // Title
        canvas.drawText("Auris - Patient Analytics Report", marginLeft, y, titlePaint)
        y += 30f
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 20f

        // Patient info
        canvas.drawText("Patient: $patientName", marginLeft, y, headerPaint)
        y += 20f
        canvas.drawText("Date: $patientDate", marginLeft, y, bodyPaint)
        y += 18f
        canvas.drawText("Screening: $screeningType  |  Score: $totalScore/$maxScore  |  Severity: $severityLabel", marginLeft, y, bodyPaint)
        y += 30f
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 25f

        // Question breakdown
        canvas.drawText("Screening Question Breakdown", marginLeft, y, headerPaint)
        y += 22f

        for (qr in questionResults) {
            canvas = checkNewPage(70f)
            canvas.drawText("Q${qr.questionNumber}: ${qr.questionText}", marginLeft, y, boldBodyPaint)
            y += 16f
            canvas.drawText("Answer: ${qr.answerText} (${qr.answerScore})", marginLeft + 10f, y, bodyPaint)
            y += 16f

            if (qr.isDiscrepancy || qr.answerScore >= 2) {
                canvas.drawText("HR: ${qr.heartRate} bpm  |  RR: ${qr.respRate} br/min", marginLeft + 10f, y, bodyPaint)
                y += 16f
                if (qr.isDiscrepancy) {
                    canvas.drawText("⚠ DISCREPANCY - ${qr.discrepancyNote.replace("\n", " ")}", marginLeft + 10f, y, redPaint)
                    y += 16f
                } else {
                    canvas.drawText("✓ Vitals consistent", marginLeft + 10f, y, greenPaint)
                    y += 16f
                }
            }
            y += 8f
        }

        // Session notes (if saved)
        val prefs = getSharedPreferences("session_notes", Context.MODE_PRIVATE)
        val notesKey = "notes_${patientName}_${screeningType}"
        val savedNotes = prefs.getString(notesKey, null)
        if (!savedNotes.isNullOrBlank()) {
            y += 10f
            canvas = checkNewPage(60f)
            canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
            y += 20f
            canvas.drawText("Session Notes", marginLeft, y, headerPaint)
            y += 20f
            // Word-wrap notes
            val words = savedNotes.split(" ")
            var line = ""
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (bodyPaint.measureText(test) > marginRight - marginLeft - 20f) {
                    canvas = checkNewPage(16f)
                    canvas.drawText(line, marginLeft + 10f, y, bodyPaint)
                    y += 16f
                    line = word
                } else {
                    line = test
                }
            }
            if (line.isNotEmpty()) {
                canvas = checkNewPage(16f)
                canvas.drawText(line, marginLeft + 10f, y, bodyPaint)
                y += 16f
            }
        }

        // Footer
        y += 20f
        canvas = checkNewPage(30f)
        canvas.drawLine(marginLeft, y, marginRight, y, linePaint)
        y += 16f
        canvas.drawText("Generated by Auris | Clinical decision support only. Therapist judgment takes precedence.", marginLeft, y, mutedPaint)

        document.finishPage(page)

        // Save to cache dir and share
        try {
            val fileName = "Auris_${patientName.replace(" ", "_")}_${screeningType}_Report.pdf"
            val file = File(cacheDir, fileName)
            document.writeTo(FileOutputStream(file))
            document.close()

            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Auris Report - $patientName ($screeningType)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Export PDF"))
        } catch (e: Exception) {
            document.close()
            Toast.makeText(this, "Error creating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── Session Notes Dialog ──────────────────────────────────────────────
    private fun showSessionNotesDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_session_notes, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)
        val tvPatient = dialogView.findViewById<TextView>(R.id.tvNotesPatient)
        val tvTimestamp = dialogView.findViewById<TextView>(R.id.tvSavedTimestamp)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<TextView>(R.id.btnSave)

        tvPatient.text = "$patientName • $screeningType • $patientDate"

        // Load previously saved notes
        val prefs = getSharedPreferences("session_notes", Context.MODE_PRIVATE)
        val notesKey = "notes_${patientName}_${screeningType}"
        val timestampKey = "timestamp_${patientName}_${screeningType}"
        val existing = prefs.getString(notesKey, "")
        val savedTime = prefs.getString(timestampKey, null)

        if (!existing.isNullOrBlank()) {
            etNotes.setText(existing)
            etNotes.setSelection(existing.length)
        }
        if (savedTime != null) {
            tvTimestamp.text = "Last saved: $savedTime"
            tvTimestamp.visibility = View.VISIBLE
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val notes = etNotes.text.toString().trim()
            if (notes.isEmpty()) {
                etNotes.error = "Please enter some notes"
                return@setOnClickListener
            }
            val now = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()).format(Date())
            prefs.edit()
                .putString(notesKey, notes)
                .putString(timestampKey, now)
                .apply()
            Toast.makeText(this, "Session notes saved", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getSeverityLabel(type: String, score: Int): String {
        return if (type == "PHQ-9") {
            when {
                score >= 20 -> "Critical"
                score >= 15 -> "Severe"
                score >= 10 -> "Moderate"
                score >= 5 -> "Mild"
                else -> "Minimal"
            }
        } else {
            when {
                score >= 15 -> "Severe"
                score >= 10 -> "Moderate"
                score >= 5 -> "Mild"
                else -> "Minimal"
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun generateSampleData(type: String, totalScore: Int): List<QuestionResult> {
        val phq9Questions = listOf(
            "Little interest or pleasure in doing things",
            "Feeling down, depressed, or hopeless",
            "Trouble falling or staying asleep, or sleeping too much",
            "Feeling tired or having little energy",
            "Poor appetite or overeating",
            "Feeling bad about yourself",
            "Trouble concentrating on things",
            "Moving or speaking slowly, or being fidgety",
            "Thoughts that you would be better off dead"
        )

        val gad7Questions = listOf(
            "Feeling nervous, anxious or on edge",
            "Not being able to stop or control worrying",
            "Worrying too much about different things",
            "Trouble relaxing",
            "Being so restless that it is hard to sit still",
            "Becoming easily annoyed or irritable",
            "Feeling afraid as if something awful might happen"
        )

        val questions = if (type == "PHQ-9") phq9Questions else gad7Questions
        val answerTexts = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")

        // Distribute the total score across questions
        val numQ = questions.size
        val scores = MutableList(numQ) { 0 }
        var remaining = totalScore.coerceAtMost(numQ * 3)
        for (i in 0 until numQ) {
            if (remaining <= 0) break
            val maxForThis = minOf(3, remaining)
            scores[i] = if (i < numQ - 1) minOf(maxForThis, (remaining / (numQ - i)) + 1) else remaining
            scores[i] = scores[i].coerceIn(0, 3)
            remaining -= scores[i]
        }

        // Generate vitals - mostly normal, with some elevated for high-score answers
        val results = mutableListOf<QuestionResult>()
        for (i in questions.indices) {
            val baseHR = 72 + (scores[i] * 5) + (i % 3) * 2
            val baseRR = 14 + (scores[i] * 1) + (i % 2)

            // Create a discrepancy on the last question if score is 0 but vitals are high
            // Or if it's a sensitive question (Q9 for PHQ-9, Q7 for GAD-7) with low score but high vitals
            val isSensitiveQ = (type == "PHQ-9" && i == 8) || (type == "GAD-7" && i == 6)
            val isDiscrepancy = isSensitiveQ && scores[i] == 0 && totalScore > 10

            val hr = if (isDiscrepancy) 96 else baseHR
            val rr = if (isDiscrepancy) 28 else baseRR

            val discNote = if (isDiscrepancy) {
                if (type == "PHQ-9")
                    "Patient denied self-harm but showed strongest stress response:\n• HR ${((hr - 75) * 100 / 75)}% above average\n• RR ${((rr - 15) * 100 / 15)}% above average"
                else
                    "Patient reported no fear but vitals showed elevated stress:\n• HR ${((hr - 75) * 100 / 75)}% above average\n• RR ${((rr - 15) * 100 / 15)}% above average"
            } else ""

            results.add(
                QuestionResult(
                    questionNumber = i + 1,
                    questionText = questions[i],
                    answerText = answerTexts[scores[i]],
                    answerScore = scores[i],
                    heartRate = hr,
                    respRate = rr,
                    isDiscrepancy = isDiscrepancy,
                    discrepancyNote = discNote
                )
            )
        }

        return results
    }
}
