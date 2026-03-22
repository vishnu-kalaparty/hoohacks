package com.example.cadence

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cadence.api.CheckinSubmitRequest
import com.example.cadence.api.CheckinSubmitResponse
import com.example.cadence.api.QuestionResponseItem
import com.example.cadence.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResultsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ResultsActivity"
        private const val PREFS_NAME = "cadence_auth"
    }

    private lateinit var btnDone: Button
    private lateinit var seekDistress: SeekBar
    private lateinit var etSituation: EditText
    private lateinit var etCoping: EditText
    private var submitInFlight = false

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
        btnDone = findViewById(R.id.btnDone)
        seekDistress = findViewById(R.id.seekDistress)
        etSituation = findViewById(R.id.etSituation)
        etCoping = findViewById(R.id.etCoping)

        val isPHQ9 = screeningType == SelectScreeningActivity.TYPE_PHQ9
        val maxScore = if (isPHQ9) 27 else 21

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

        tvScore.text = totalScore.toString()
        tvScoreMax.text = "/$maxScore"

        val severity = getSeverity(totalScore, isPHQ9)
        tvSeverity.text = severity.label
        tvSeverity.setTextColor(getColor(severity.colorRes))

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
            if (submitInFlight) return@setOnClickListener
            submitCheckinAndFinish(screeningType, totalScore)
        }

        findViewById<View>(R.id.btnDownload).setOnClickListener {
            Toast.makeText(this, "Download feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Runs after screening + breathing flow. Sends one payload: per-question scores & vitals,
     * aggregate vitals, distress, situation/coping text.
     */
    private fun submitCheckinAndFinish(screeningType: String, totalScore: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val patientId = prefs.getInt("patient_id", -1)

        val scoresArray = intent.getIntArrayExtra("scores_array")
        val hrvArray = intent.getDoubleArrayExtra("hrv_array")
        val questionIds = intent.getStringArrayExtra("question_ids")
        val avgHrv = intent.getDoubleExtra("avg_hrv", 0.0)
        val avgBreathing = intent.getDoubleExtra("avg_breathing", 0.0)
        val lastPulse = intent.getDoubleExtra("last_pulse", 0.0)

        if (scoresArray == null || questionIds == null) {
            Toast.makeText(
                this,
                "Screening data incomplete; cannot send check-in.",
                Toast.LENGTH_LONG
            ).show()
            Log.w(TAG, "Missing scores_array or question_ids")
            return
        }

        val scaleType = if (screeningType == SelectScreeningActivity.TYPE_PHQ9) "PHQ-9" else "GAD-7"
        val distress = seekDistress.progress
        val situation = etSituation.text?.toString()?.trim().orEmpty()
        val coping = etCoping.text?.toString()?.trim().orEmpty()

        val questionResponses = questionIds.indices.map { i ->
            QuestionResponseItem(
                question_id = questionIds[i],
                response = scoresArray.getOrElse(i) { 0 },
                hrv_at_question = hrvArray?.getOrElse(i) { 0.0 }.takeIf { it != null && it > 0.0 }
            )
        }

        val request = CheckinSubmitRequest(
            patient_id = if (patientId > 0) patientId else 1,
            scale_type = scaleType,
            scale_score = totalScore,
            hrv = avgHrv,
            breathing_rate = avgBreathing,
            pulse_rate = lastPulse,
            distress = distress,
            situation = situation,
            coping = coping,
            questions = questionResponses
        )

        submitInFlight = true
        btnDone.isEnabled = false
        btnDone.text = getString(R.string.results_sending)

        RetrofitClient.api.submitCheckin(request).enqueue(object : Callback<CheckinSubmitResponse> {
            override fun onResponse(call: Call<CheckinSubmitResponse>, response: Response<CheckinSubmitResponse>) {
                submitInFlight = false
                btnDone.isEnabled = true
                btnDone.text = getString(R.string.results_btn_done)

                if (response.isSuccessful && response.body()?.success == true) {
                    val sessionId = response.body()?.session_id
                    Log.i(TAG, "Check-in submitted, session_id=$sessionId")
                    Toast.makeText(this@ResultsActivity, "Check-in sent.", Toast.LENGTH_SHORT).show()
                    goHome()
                } else {
                    Log.w(TAG, "Check-in returned ${response.code()}")
                    Toast.makeText(
                        this@ResultsActivity,
                        "Could not send check-in (${response.code()}). Try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<CheckinSubmitResponse>, t: Throwable) {
                submitInFlight = false
                btnDone.isEnabled = true
                btnDone.text = getString(R.string.results_btn_done)
                Log.w(TAG, "Check-in failed", t)
                Toast.makeText(
                    this@ResultsActivity,
                    "Network error. Check-in not sent. Try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun goHome() {
        val intent = android.content.Intent(this, MainActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
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
