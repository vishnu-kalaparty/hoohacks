package com.example.cadence

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PatientListActivity : AppCompatActivity() {

    data class Patient(
        val name: String,
        val date: String,
        val screeningType: String, // "PHQ-9" or "GAD-7"
        val score: Int,
        val severity: String // "Critical", "Severe", "Moderate", "Mild"
    )

    private lateinit var chips: List<TextView>
    private lateinit var patientCardsContainer: LinearLayout
    private lateinit var tvTotalCount: TextView
    private var activeChip: String = "All"
    private var currentFilter: String = "All" // All, PHQ-9, GAD-7
    private var currentSort: String = "Date" // Date, Score, Name

    private val patients = listOf(
        Patient("Sarah Mitchell", "Jan 15, 2025 • 2:30 PM", "PHQ-9", 22, "Critical"),
        Patient("James Anderson", "Jan 15, 2025 • 1:15 PM", "GAD-7", 16, "Severe"),
        Patient("Emily Rodriguez", "Jan 15, 2025 • 11:45 AM", "PHQ-9", 12, "Moderate"),
        Patient("Michael Chen", "Jan 14, 2025 • 3:00 PM", "GAD-7", 8, "Mild"),
        Patient("Lisa Thompson", "Jan 14, 2025 • 10:30 AM", "PHQ-9", 19, "Severe"),
        Patient("David Park", "Jan 13, 2025 • 4:15 PM", "GAD-7", 14, "Moderate"),
        Patient("Anna Williams", "Jan 13, 2025 • 9:00 AM", "PHQ-9", 24, "Critical"),
        Patient("Robert Garcia", "Jan 12, 2025 • 2:00 PM", "GAD-7", 5, "Mild")
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

        // Filter button
        findViewById<View>(R.id.btnFilter).setOnClickListener {
            showFilterDialog()
        }

        // Sort button
        findViewById<View>(R.id.btnSort).setOnClickListener {
            showSortDialog()
        }

        displayPatients()
    }

    private fun showFilterDialog() {
        val options = arrayOf("All Screenings", "PHQ-9 Only", "GAD-7 Only")
        val currentIndex = when (currentFilter) {
            "All" -> 0
            "PHQ-9" -> 1
            "GAD-7" -> 2
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("Filter by Screening Type")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                currentFilter = when (which) {
                    0 -> "All"
                    1 -> "PHQ-9"
                    2 -> "GAD-7"
                    else -> "All"
                }
                displayPatients()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSortDialog() {
        val options = arrayOf("Most Recent", "Highest Score", "Name (A-Z)")
        val currentIndex = when (currentSort) {
            "Date" -> 0
            "Score" -> 1
            "Name" -> 2
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("Sort Patients")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                currentSort = when (which) {
                    0 -> "Date"
                    1 -> "Score"
                    2 -> "Name"
                    else -> "Date"
                }
                displayPatients()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        // Apply severity filter (chips)
        var filtered = if (activeChip == "All") patients
        else patients.filter { it.severity == activeChip }

        // Apply screening type filter
        if (currentFilter != "All") {
            filtered = filtered.filter { it.screeningType == currentFilter }
        }

        // Apply sorting
        filtered = when (currentSort) {
            "Score" -> filtered.sortedByDescending { it.score }
            "Name" -> filtered.sortedBy { it.name }
            else -> filtered // Date is already in chronological order
        }

        tvTotalCount.text = filtered.size.toString()

        for (patient in filtered) {
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.item_patient_card, patientCardsContainer, false)

            cardView.findViewById<TextView>(R.id.tvPatientName).text = patient.name
            cardView.findViewById<TextView>(R.id.tvPatientDate).text = patient.date

            // Score label and value
            val tvScoreLabel = cardView.findViewById<TextView>(R.id.tvScoreLabel)
            val tvScoreValue = cardView.findViewById<TextView>(R.id.tvScoreValue)
            tvScoreLabel.text = if (patient.screeningType == "PHQ-9")
                getString(R.string.plist_phq9_score) else getString(R.string.plist_gad7_score)
            tvScoreValue.text = patient.score.toString()

            // Color the score based on severity
            val scoreColor = when (patient.severity) {
                "Critical" -> 0xFFDC2626.toInt()
                "Severe" -> 0xFFEA580C.toInt()
                "Moderate" -> 0xFF059669.toInt()
                else -> 0xFF0284C7.toInt()
            }
            tvScoreValue.setTextColor(scoreColor)

            // Severity badge
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

            // View Analytics button
            cardView.findViewById<View>(R.id.btnViewAnalytics).setOnClickListener {
                val intent = Intent(this, PatientAnalyticsActivity::class.java)
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
