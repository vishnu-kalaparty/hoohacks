package com.example.cadence

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.util.Log
import com.example.cadence.api.Patient
import com.example.cadence.api.PlaceholderData
import com.example.cadence.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TherapistPatientsActivity : AppCompatActivity() {

    private lateinit var rvPatients: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val therapistId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_therapist_patients)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvPatients = findViewById(R.id.rvPatients)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        rvPatients.layoutManager = LinearLayoutManager(this)

        swipeRefresh.setOnRefreshListener { loadPatients() }
        loadPatients()
    }

    private fun loadPatients() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            val patients = try {
                val response = RetrofitClient.api.getPatients(therapistId)
                response.patients
            } catch (e: Exception) {
                Log.w("TherapistPatients", "API unavailable, using placeholders", e)
                PlaceholderData.patients
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false

                if (patients.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rvPatients.adapter = PatientAdapter(patients) { patient ->
                        val intent = Intent(
                            this@TherapistPatientsActivity,
                            SimilarSessionsActivity::class.java
                        )
                        intent.putExtra("patient_id", patient.patientId)
                        intent.putExtra("patient_name", patient.name)
                        intent.putExtra("assigned_scale", patient.assignedScale)
                        startActivity(intent)
                    }
                }
            }
        }
    }
}


class PatientAdapter(
    private val patients: List<Patient>,
    private val onClick: (Patient) -> Unit
) : RecyclerView.Adapter<PatientAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPatientName)
        val tvScale: TextView = view.findViewById(R.id.tvPatientScale)
        val tvCheckins: TextView = view.findViewById(R.id.tvCheckinCount)
        val tvScore: TextView = view.findViewById(R.id.tvLatestScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = patients[position]
        holder.tvName.text = p.name
        holder.tvScale.text = p.assignedScale
        holder.tvCheckins.text = "${p.checkinCount} check-ins"
        holder.tvScore.text = p.latestScore?.toString() ?: "--"
        holder.itemView.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = patients.size
}
