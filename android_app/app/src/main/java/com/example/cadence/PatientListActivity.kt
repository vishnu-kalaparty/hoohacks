package com.example.cadence

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cadence.api.ApiPatient
import com.example.cadence.api.PatientListResponse
import com.example.cadence.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PatientListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PatientList"
    }

    data class Patient(
        val id: Int,
        val name: String,
        val date: String,
        val screeningType: String,
        val score: Int,
        val severity: String
    )

    private lateinit var chips: List<TextView>
    private lateinit var patientCardsContainer: LinearLayout
    private lateinit var tvTotalCount: TextView
    private var activeChip: String = "All"
    private var therapistId: Int = -1

    private var patients: List<Patient> = emptyList()

    private val fallbackPatients = listOf(
        Patient(1, "Sarah Mitchell", "Jan 15, 2025 • 2:30 PM", "PHQ-9", 22, "Critical"),
        Patient(2, "James Anderson", "Jan 15, 2025 • 1:15 PM", "GAD-7", 16, "Severe"),
        Patient(3, "Emily Rodriguez", "Jan 15, 2025 • 11:45 AM", "PHQ-9", 12, "Moderate"),
        Patient(4, "Michael Chen", "Jan 14, 2025 • 3:00 PM", "GAD-7", 8, "Mild"),
        Patient(5, "Lisa Thompson", "Jan 14, 2025 • 10:30 AM", "PHQ-9", 19, "Severe"),
        Patient(6, "David Park", "Jan 13, 2025 • 4:15 PM", "GAD-7", 14, "Moderate"),
        Patient(7, "Anna Williams", "Jan 13, 2025 • 9:00 AM", "PHQ-9", 24, "Critical"),
        Patient(8, "Robert Garcia", "Jan 12, 2025 • 2:00 PM", "GAD-7", 5, "Mild")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        therapistId = intent.getIntExtra("therapist_id", -1)
        patientCardsContainer = findViewById(R.id.patientCardsContainer)
        tvTotalCount = findViewById(R.id.tvTotalCount)

        val chipAll = findViewById<TextView>(R.id.chipAll)
        val chipCritical = findViewById<TextView>(R.id.chipCritical)
        val chipSevere = findViewById<TextView>(R.id.chipSevere)
        val chipModerate = findViewById<TextView>(R.id.chipModerate)
        val chipMild = findViewById<TextView>(R.id.chipMild)

        chips = listOf(chipAll, chipCritical, chipSevere, chipModerate, chipMild)
        val chipLabels = listOf("All", "Critical", "Severe", "Moderate", "Mild")

        for (i in chips.indices) {
            chips[i].setOnClickListener {
                activeChip = chipLabels[i]
                updateChipStates()
                displayPatients()
            }
        }

        loadPatients()
    }

    private fun loadPatients() {
        if (therapistId <= 0) {
            patients = fallbackPatients
            displayPatients()
            return
        }

        RetrofitClient.api.getPatients(therapistId).enqueue(object : Callback<PatientListResponse> {
            override fun onResponse(call: Call<PatientListResponse>, response: Response<PatientListResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val apiPatients = response.body()!!.patients
                    patients = apiPatients.map { it.toLocalPatient() }
                    if (patients.isEmpty()) patients = fallbackPatients
                } else {
                    Log.w(TAG, "API returned ${response.code()}, using fallback")
                    patients = fallbackPatients
                }
                displayPatients()
            }

            override fun onFailure(call: Call<PatientListResponse>, t: Throwable) {
                Log.w(TAG, "API unreachable, using fallback", t)
                patients = fallbackPatients
                displayPatients()
            }
        })
    }

    private fun ApiPatient.toLocalPatient(): Patient {
        val score = latestScore ?: 0
        val scale = assignedScale ?: "PHQ-9"
        val severity = getSeverity(score, scale)
        val dateStr = latestCheckin ?: "No check-ins"
        return Patient(
            id = patientId,
            name = name,
            date = dateStr,
            screeningType = scale,
            score = score,
            severity = severity
        )
    }

    private fun getSeverity(score: Int, scaleType: String): String {
        return if (scaleType == "PHQ-9") {
            when {
                score >= 20 -> "Critical"
                score >= 15 -> "Severe"
                score >= 10 -> "Moderate"
                score >= 5 -> "Mild"
                else -> "Mild"
            }
        } else {
            when {
                score >= 15 -> "Severe"
                score >= 10 -> "Moderate"
                score >= 5 -> "Mild"
                else -> "Mild"
            }
        }
    }

    private fun updateChipStates() {
        val chipLabels = listOf("All", "Critical", "Severe", "Moderate", "Mild")
        for (i in chips.indices) {
            if (chipLabels[i] == activeChip) {
                chips[i].setBackgroundResource(R.drawable.chip_active)
                chips[i].setTextColor(getColor(R.color.text_primary))
            } else {
                chips[i].setBackgroundResource(R.drawable.chip_inactive)
                chips[i].setTextColor(getColor(R.color.text_dark))
            }
        }
    }

    private fun displayPatients() {
        patientCardsContainer.removeAllViews()

        val filtered = if (activeChip == "All") patients
        else patients.filter { it.severity == activeChip }

        tvTotalCount.text = filtered.size.toString()

        for (patient in filtered) {
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_patient_card, patientCardsContainer, false)

            cardView.findViewById<TextView>(R.id.tvPatientName).text = patient.name
            cardView.findViewById<TextView>(R.id.tvPatientDate).text = patient.date

            val tvScoreLabel = cardView.findViewById<TextView>(R.id.tvScoreLabel)
            val tvScoreValue = cardView.findViewById<TextView>(R.id.tvScoreValue)
            tvScoreLabel.text = if (patient.screeningType == "PHQ-9")
                getString(R.string.plist_phq9_score) else getString(R.string.plist_gad7_score)
            tvScoreValue.text = patient.score.toString()

            val scoreColor = when (patient.severity) {
                "Critical" -> 0xFFDC2626.toInt()
                "Severe" -> 0xFFEA580C.toInt()
                "Moderate" -> 0xFF059669.toInt()
                else -> 0xFF0284C7.toInt()
            }
            tvScoreValue.setTextColor(scoreColor)

            val tvBadge = cardView.findViewById<TextView>(R.id.tvSeverityBadge)
            tvBadge.text = patient.severity
            when (patient.severity) {
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

            cardView.findViewById<View>(R.id.btnViewAnalytics).setOnClickListener {
                val intent = Intent(this, PatientAnalyticsActivity::class.java)
                intent.putExtra("patient_id", patient.id)
                intent.putExtra("patient_name", patient.name)
                intent.putExtra("patient_date", patient.date)
                intent.putExtra("screening_type", patient.screeningType)
                intent.putExtra("score", patient.score)
                intent.putExtra("severity", patient.severity)
                startActivity(intent)
            }

            patientCardsContainer.addView(cardView)
        }
    }
}
