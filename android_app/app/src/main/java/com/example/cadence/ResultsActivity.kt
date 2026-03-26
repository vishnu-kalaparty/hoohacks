package com.example.cadence

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_results)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val totalScore = intent.getIntExtra("total_score", 0)
        val screeningType = intent.getStringExtra("screening_type")
            ?: SelectScreeningActivity.TYPE_PHQ9

        val tvScaleTitle = findViewById<TextView>(R.id.tvScaleTitle)
        val tvScaleLabel = findViewById<TextView>(R.id.tvScaleLabel)
        val tvScore = findViewById<TextView>(R.id.tvScore)
        val tvScoreMax = findViewById<TextView>(R.id.tvScoreMax)
        val tvSeverity = findViewById<TextView>(R.id.tvSeverity)
        val scoreBarFill = findViewById<View>(R.id.scoreBarFill)
        val scoreAccent = findViewById<View>(R.id.scoreAccent)
        val ivScaleIcon = findViewById<ImageView>(R.id.ivScaleIcon)
        val btnDone = findViewById<Button>(R.id.btnDone)

        val isPHQ9 = screeningType == SelectScreeningActivity.TYPE_PHQ9
        val maxScore = if (isPHQ9) 27 else 21

        // Set scale title and label
        if (isPHQ9) {
            tvScaleTitle.text = getString(R.string.results_phq9_scale)
            tvScaleLabel.text = getString(R.string.results_phq9_label)
            ivScaleIcon.setImageResource(R.drawable.ic_mood)
            scoreAccent.setBackgroundResource(R.color.icon_teal)
        } else {
            tvScaleTitle.text = getString(R.string.results_gad7_scale)
            tvScaleLabel.text = getString(R.string.results_gad7_label)
            ivScaleIcon.setImageResource(R.drawable.ic_heart_blue)
            scoreAccent.setBackgroundResource(R.color.info_blue)
        }

        // Set score
        tvScore.text = totalScore.toString()
        tvScoreMax.text = "/$maxScore"

        // Calculate severity
        val severity = getSeverity(totalScore, isPHQ9)
        tvSeverity.text = severity.label
        tvSeverity.setTextColor(getColor(severity.colorRes))

        // Animate score bar
        scoreBarFill.post {
            val parent = scoreBarFill.parent as View
            val params = scoreBarFill.layoutParams
            val fraction = totalScore.toFloat() / maxScore
            params.width = (parent.width * fraction).toInt()
            scoreBarFill.layoutParams = params
            scoreBarFill.setBackgroundResource(
                if (isPHQ9) R.drawable.score_bar_fill_teal else R.drawable.score_bar_fill_blue
            )
        }

        btnDone.setOnClickListener {
            // Navigate back to home
            val intent = android.content.Intent(this, MainActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.btnDownload).setOnClickListener {
            Toast.makeText(this, "Download feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private data class Severity(val label: String, val colorRes: Int)

    private fun getSeverity(score: Int, isPHQ9: Boolean): Severity {
        return if (isPHQ9) {
            when {
                score <= 4 -> Severity("Minimal Depression", R.color.icon_teal)
                score <= 9 -> Severity("Mild Depression", R.color.icon_teal)
                score <= 14 -> Severity("Moderate Depression", R.color.info_blue)
                score <= 19 -> Severity("Moderately Severe Depression", R.color.info_blue)
                else -> Severity("Severe Depression", R.color.info_blue)
            }
        } else {
            when {
                score <= 4 -> Severity("Minimal Anxiety", R.color.icon_teal)
                score <= 9 -> Severity("Mild Anxiety", R.color.icon_teal)
                score <= 14 -> Severity("Moderate Anxiety", R.color.info_blue)
                else -> Severity("Severe Anxiety", R.color.info_blue)
            }
        }
    }
}
