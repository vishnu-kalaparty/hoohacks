package com.example.cadence

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import com.example.cadence.api.PlaceholderData
import com.example.cadence.api.RetrofitClient
import com.example.cadence.api.SimilarSession
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class SimilarSessionsActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var rvSessions: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPatientName: TextView
    private lateinit var tvMatchCount: TextView

    private var patientId = 0
    private var patientName = ""
    private var sessions: List<SimilarSession> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_similar_sessions)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        patientId = intent.getIntExtra("patient_id", 0)
        patientName = intent.getStringExtra("patient_name") ?: "Patient"

        lineChart = findViewById(R.id.lineChart)
        rvSessions = findViewById(R.id.rvSessions)
        progressBar = findViewById(R.id.progressBar)
        tvPatientName = findViewById(R.id.tvPatientName)
        tvMatchCount = findViewById(R.id.tvMatchCount)

        tvPatientName.text = patientName
        rvSessions.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        setupChart()
        loadSimilarSessions()
    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            setNoDataText("Loading similar patterns...")
            setNoDataTextColor(Color.WHITE)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.parseColor("#E0F2F7")
                textSize = 10f
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.apply {
                textColor = Color.parseColor("#E0F2F7")
                textSize = 11f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#33FFFFFF")
                axisMinimum = 0f
            }
            axisRight.isEnabled = false

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val idx = it.data as? Int ?: return
                        if (idx in sessions.indices) {
                            openSessionDetail(sessions[idx].sessionId)
                        }
                    }
                }
                override fun onNothingSelected() {}
            })
        }
    }

    private fun loadSimilarSessions() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val fetched = try {
                val response = RetrofitClient.api.getSimilarSessions(patientId)
                response.similarSessions
            } catch (e: Exception) {
                Log.w("SimilarSessions", "API unavailable, using placeholders", e)
                PlaceholderData.similarSessions
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                sessions = fetched.sortedBy { it.checkinDate }
                tvMatchCount.text = "${sessions.size} similar sessions"
                populateChart(sessions)
                rvSessions.adapter = SessionAdapter(sessions) { session ->
                    openSessionDetail(session.sessionId)
                }
            }
        }
    }

    private fun populateChart(sessions: List<SimilarSession>) {
        if (sessions.isEmpty()) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFormat = SimpleDateFormat("MMM dd", Locale.US)

        val sorted = sessions.sortedBy { it.checkinDate }
        val dateLabels = mutableListOf<String>()

        val entries = sorted.mapIndexed { index, session ->
            val label = try {
                val date = dateFormat.parse(session.checkinDate.take(10))
                displayFormat.format(date!!)
            } catch (_: Exception) {
                session.checkinDate.take(10)
            }
            dateLabels.add(label)
            Entry(index.toFloat(), session.scaleScore.toFloat(), index)
        }

        val dataSet = LineDataSet(entries, "Scale Score").apply {
            color = Color.parseColor("#4ADEAC")
            setCircleColor(Color.parseColor("#4ADEAC"))
            circleRadius = 5f
            lineWidth = 2.5f
            setDrawValues(true)
            valueTextColor = Color.WHITE
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = value.toInt().toString()
            }
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#4ADEAC")
            fillAlpha = 30
            highLightColor = Color.WHITE
        }

        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx in dateLabels.indices) dateLabels[idx] else ""
            }
        }

        lineChart.data = LineData(dataSet)
        lineChart.animateX(600)
        lineChart.invalidate()
    }

    private fun openSessionDetail(sessionId: Int) {
        val intent = Intent(this, SessionDetailActivity::class.java)
        intent.putExtra("session_id", sessionId)
        startActivity(intent)
    }
}


class SessionAdapter(
    private val sessions: List<SimilarSession>,
    private val onClick: (SimilarSession) -> Unit
) : RecyclerView.Adapter<SessionAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
        val tvSituation: TextView = view.findViewById(R.id.tvSituation)
        val tvSimilarity: TextView = view.findViewById(R.id.tvSimilarity)
        val tvHrv: TextView = view.findViewById(R.id.tvHrv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = sessions[position]

        holder.tvDate.text = try {
            val date = dateFormat.parse(s.checkinDate.take(10))
            displayFormat.format(date!!)
        } catch (_: Exception) {
            s.checkinDate
        }

        holder.tvScore.text = s.scaleScore.toString()
        holder.tvSituation.text = s.situationText ?: "No situation recorded"

        val simPct = (s.similarity * 100).toInt()
        holder.tvSimilarity.text = "${simPct}% match"
        holder.tvHrv.text = s.hrvValue?.let { "HRV: %.1f".format(it) } ?: ""

        holder.itemView.setOnClickListener { onClick(s) }
    }

    override fun getItemCount() = sessions.size
}
