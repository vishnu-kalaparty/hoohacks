package com.example.cadence

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnPatient = findViewById<Button>(R.id.btnPatient)
        val btnTherapist = findViewById<Button>(R.id.btnTherapist)

        btnPatient.setOnClickListener {
            val intent = Intent(this, ConsentActivity::class.java)
            startActivity(intent)
        }

        btnTherapist.setOnClickListener {
            // Therapist flow logic can go here later
        }
    }
}