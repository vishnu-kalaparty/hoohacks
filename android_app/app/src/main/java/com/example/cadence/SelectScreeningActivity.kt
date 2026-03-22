package com.example.cadence

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SelectScreeningActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCREENING_TYPE = "screening_type"
        const val TYPE_PHQ9 = "phq9"
        const val TYPE_GAD7 = "gad7"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_screening)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnSelectBack)
        val cardPhq9 = findViewById<LinearLayout>(R.id.cardPhq9)
        val cardGad7 = findViewById<LinearLayout>(R.id.cardGad7)

        btnBack.setOnClickListener { finish() }

        cardPhq9.setOnClickListener {
            val intent = Intent(this, ScreeningActivity::class.java)
            intent.putExtra(EXTRA_SCREENING_TYPE, TYPE_PHQ9)
            startActivity(intent)
        }

        cardGad7.setOnClickListener {
            val intent = Intent(this, ScreeningActivity::class.java)
            intent.putExtra(EXTRA_SCREENING_TYPE, TYPE_GAD7)
            startActivity(intent)
        }
    }
}
