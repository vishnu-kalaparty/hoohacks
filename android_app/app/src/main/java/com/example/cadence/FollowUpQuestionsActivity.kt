package com.example.cadence

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FollowUpQuestionsActivity : AppCompatActivity() {

    private var currentStep = 0 // 0=distress, 1=stressful, 2=coping

    private var distressLevel = -1
    private var stressfulEvent = false
    private var stressDescription = ""
    private var copingResponse = ""

    private lateinit var questionDistress: LinearLayout
    private lateinit var questionStressful: LinearLayout
    private lateinit var questionCoping: LinearLayout
    private lateinit var btnNext: TextView
    private lateinit var progressFill: View

    private lateinit var ratingButtons: Array<TextView>
    private lateinit var btnYes: TextView
    private lateinit var btnNo: TextView
    private lateinit var etStressDescription: EditText
    private lateinit var etCoping: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_follow_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        questionDistress = findViewById(R.id.questionDistress)
        questionStressful = findViewById(R.id.questionStressful)
        questionCoping = findViewById(R.id.questionCoping)
        btnNext = findViewById(R.id.btnFollowUpNext)
        progressFill = findViewById(R.id.followUpProgressFill)

        btnYes = findViewById(R.id.btnYes)
        btnNo = findViewById(R.id.btnNo)
        etStressDescription = findViewById(R.id.etStressDescription)
        etCoping = findViewById(R.id.etCoping)

        ratingButtons = arrayOf(
            findViewById(R.id.rating1), findViewById(R.id.rating2),
            findViewById(R.id.rating3), findViewById(R.id.rating4),
            findViewById(R.id.rating5), findViewById(R.id.rating6),
            findViewById(R.id.rating7), findViewById(R.id.rating8),
            findViewById(R.id.rating9), findViewById(R.id.rating10)
        )

        // Back button
        findViewById<View>(R.id.btnFollowUpBack).setOnClickListener { finish() }

        // Rating buttons 1-10
        for (i in ratingButtons.indices) {
            ratingButtons[i].setOnClickListener { selectRating(i + 1) }
        }

        // Yes/No buttons
        btnYes.setOnClickListener { selectStressful(true) }
        btnNo.setOnClickListener { selectStressful(false) }

        // Next/Submit button
        btnNext.setOnClickListener { advanceStep() }

        updateUI()
    }

    private fun selectRating(level: Int) {
        distressLevel = level
        for (i in ratingButtons.indices) {
            if (i + 1 == level) {
                ratingButtons[i].setBackgroundResource(R.drawable.answer_card_selected)
                ratingButtons[i].setTextColor(0xFF059669.toInt())
            } else {
                ratingButtons[i].setBackgroundResource(R.drawable.answer_card_background)
                ratingButtons[i].setTextColor(getColor(R.color.text_dark))
            }
        }
        enableNext()
    }

    private fun selectStressful(yes: Boolean) {
        stressfulEvent = yes
        if (yes) {
            btnYes.setBackgroundResource(R.drawable.answer_card_selected)
            btnYes.setTextColor(0xFF059669.toInt())
            btnNo.setBackgroundResource(R.drawable.answer_card_background)
            btnNo.setTextColor(getColor(R.color.text_dark))
            etStressDescription.visibility = View.VISIBLE
        } else {
            btnNo.setBackgroundResource(R.drawable.answer_card_selected)
            btnNo.setTextColor(0xFF059669.toInt())
            btnYes.setBackgroundResource(R.drawable.answer_card_background)
            btnYes.setTextColor(getColor(R.color.text_dark))
            etStressDescription.visibility = View.GONE
        }
        enableNext()
    }

    private fun enableNext() {
        btnNext.alpha = 1.0f
        btnNext.isEnabled = true
    }

    private fun disableNext() {
        btnNext.alpha = 0.4f
        btnNext.isEnabled = false
    }

    private fun advanceStep() {
        when (currentStep) {
            0 -> {
                if (distressLevel < 1) return
                currentStep = 1
            }
            1 -> {
                if (stressfulEvent) {
                    stressDescription = etStressDescription.text.toString().trim()
                }
                currentStep = 2
            }
            2 -> {
                copingResponse = etCoping.text.toString().trim()
                finishFollowUp()
                return
            }
        }
        updateUI()
    }

    private fun updateUI() {
        questionDistress.visibility = if (currentStep == 0) View.VISIBLE else View.GONE
        questionStressful.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        questionCoping.visibility = if (currentStep == 2) View.VISIBLE else View.GONE

        // Update progress bar
        val fraction = (currentStep + 1).toFloat() / 3f
        progressFill.post {
            val parent = progressFill.parent as View
            val params = progressFill.layoutParams
            params.width = (parent.width * fraction).toInt()
            progressFill.layoutParams = params
        }

        // Update button text
        btnNext.text = if (currentStep == 2) getString(R.string.followup_submit) else getString(R.string.followup_next)

        // Enable/disable next based on current step state
        when (currentStep) {
            0 -> if (distressLevel < 1) disableNext() else enableNext()
            1 -> disableNext() // require yes/no selection
            2 -> enableNext() // free-text, always enabled
        }
    }

    private fun finishFollowUp() {
        val resultIntent = Intent(this, BreatheActivity::class.java)
        // Pass through screening data
        resultIntent.putExtra("total_score", intent.getIntExtra("total_score", 0))
        resultIntent.putExtra("screening_type", intent.getStringExtra("screening_type")
            ?: SelectScreeningActivity.TYPE_PHQ9)
        // Pass follow-up answers
        resultIntent.putExtra("followup_distress", distressLevel)
        resultIntent.putExtra("followup_stressful", stressfulEvent)
        resultIntent.putExtra("followup_stress_desc", stressDescription)
        resultIntent.putExtra("followup_coping", copingResponse)
        startActivity(resultIntent)
        finish()
    }
}
