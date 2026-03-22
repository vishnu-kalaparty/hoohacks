package com.example.cadence

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ConsentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_consent)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // In production, screening type would be provided by backend API
        val screeningType = SelectScreeningActivity.TYPE_PHQ9 // Backend will provide this
        val questionCount = if (screeningType == SelectScreeningActivity.TYPE_PHQ9) 9 else 7
        val screeningName = if (screeningType == SelectScreeningActivity.TYPE_PHQ9) "PHQ-9" else "GAD-7"

        val checkboxCamera = findViewById<CheckBox>(R.id.checkboxCamera)
        val checkboxDisclaimer = findViewById<CheckBox>(R.id.checkboxDisclaimer)
        val btnBeginScreening = findViewById<Button>(R.id.btnBeginScreening)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val progressFill = findViewById<View>(R.id.progressFill)
        val tvExpectBody = findViewById<android.widget.TextView>(R.id.tvExpectBody)

        // Update question count dynamically based on screening type
        tvExpectBody.text = android.text.Html.fromHtml(
            "You'll answer <b>$questionCount questions</b> about how you've been feeling. Your camera will capture some health data during the process to provide accurate screening results.",
            android.text.Html.FROM_HTML_MODE_LEGACY
        )

        // Set progress bar to 33% (step 1 of 3)
        progressFill.post {
            val parent = progressFill.parent as View
            val params = progressFill.layoutParams
            params.width = (parent.width * 0.33).toInt()
            progressFill.layoutParams = params
        }

        // Enable Begin Screening button only when both checkboxes are checked
        val updateButtonState = {
            val bothChecked = checkboxCamera.isChecked && checkboxDisclaimer.isChecked
            btnBeginScreening.isEnabled = bothChecked
            btnBeginScreening.alpha = if (bothChecked) 1.0f else 0.5f
        }

        checkboxCamera.setOnCheckedChangeListener { _, _ -> updateButtonState() }
        checkboxDisclaimer.setOnCheckedChangeListener { _, _ -> updateButtonState() }

        btnBeginScreening.setOnClickListener {
            val intent = Intent(this, ScreeningActivity::class.java)
            intent.putExtra(SelectScreeningActivity.EXTRA_SCREENING_TYPE, screeningType)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
