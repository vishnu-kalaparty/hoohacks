package com.example.cadence

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cadence.api.LoginResponse
import com.example.cadence.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TherapistLoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TherapistLogin"
        private const val PREFS_NAME = "cadence_auth"
    }

    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_therapist_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnTogglePassword = findViewById<ImageView>(R.id.btnTogglePassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedEmail = prefs.getString("email", null)
        if (savedEmail != null) etEmail.setText(savedEmail)

        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Password is required"
                return@setOnClickListener
            }

            btnSignIn.isEnabled = false
            btnSignIn.text = "Signing in..."

            prefs.edit().putString("email", email).apply()

            tryApiLogin(email, btnSignIn)
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryApiLogin(email: String, btnSignIn: Button) {
        RetrofitClient.api.login().enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.needs_registration == false && body.user != null) {
                        val user = body.user
                        val therapistId = user.therapist_id ?: -1

                        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        prefs.edit()
                            .putInt("therapist_id", therapistId)
                            .putString("role", user.role)
                            .putString("user_name", user.name)
                            .apply()

                        navigateToPatientList(therapistId)
                    } else {
                        Log.w(TAG, "User needs registration or not found, using demo mode")
                        navigateDemoMode()
                    }
                } else {
                    Log.w(TAG, "Login API returned ${response.code()}, using demo mode")
                    navigateDemoMode()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.w(TAG, "Login API unreachable, using demo mode", t)
                navigateDemoMode()
            }
        })
    }

    private fun navigateToPatientList(therapistId: Int) {
        val intent = Intent(this, PatientListActivity::class.java)
        intent.putExtra("therapist_id", therapistId)
        startActivity(intent)
        finish()
    }

    private fun navigateDemoMode() {
        Toast.makeText(this, "Demo mode — backend not connected", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, PatientListActivity::class.java)
        intent.putExtra("therapist_id", -1)
        startActivity(intent)
        finish()
    }
}
