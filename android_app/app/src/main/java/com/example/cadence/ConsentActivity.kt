package com.example.cadence

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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

        val checkboxCamera = findViewById<CheckBox>(R.id.checkboxCamera)
        val checkboxDisclaimer = findViewById<CheckBox>(R.id.checkboxDisclaimer)
        val btnBeginScreening = findViewById<Button>(R.id.btnBeginScreening)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val progressFill = findViewById<View>(R.id.progressFill)

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
            val intent = Intent(this, SelectScreeningActivity::class.java)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
