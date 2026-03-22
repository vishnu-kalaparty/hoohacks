package com.example.cadence

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class VitalsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E8B8B")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E8B8B")
        style = Paint.Style.FILL
    }

    private val highlightDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DC2626")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E5E7EB")
        strokeWidth = 1f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9CA3AF")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9CA3AF")
        textSize = 26f
        textAlign = Paint.Align.RIGHT
    }

    private var dataPoints: List<Float> = listOf()
    private var labels: List<String> = listOf()
    private var highlightIndex: Int = -1
    private var minValue: Float = 50f
    private var maxValue: Float = 110f
    private var normalRangeMin: Float = 60f
    private var normalRangeMax: Float = 100f

    private val normalRangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E8B8B")
        alpha = 15
        style = Paint.Style.FILL
    }

    fun setData(
        points: List<Float>,
        xLabels: List<String>,
        highlight: Int = -1,
        minVal: Float = 50f,
        maxVal: Float = 110f,
        normalMin: Float = 60f,
        normalMax: Float = 100f
    ) {
        dataPoints = points
        labels = xLabels
        highlightIndex = highlight
        minValue = minVal
        maxValue = maxVal
        normalRangeMin = normalMin
        normalRangeMax = normalMax
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val paddingLeft = 60f
        val paddingRight = 30f
        val paddingTop = 20f
        val paddingBottom = 50f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val range = maxValue - minValue

        // Draw normal range band
        val normalTop = paddingTop + chartHeight * (1f - (normalRangeMax - minValue) / range)
        val normalBottom = paddingTop + chartHeight * (1f - (normalRangeMin - minValue) / range)
        canvas.drawRect(paddingLeft, normalTop, width - paddingRight, normalBottom, normalRangePaint)

        // Draw y-axis grid lines and labels
        val ySteps = 4
        for (i in 0..ySteps) {
            val value = minValue + (range * i / ySteps)
            val y = paddingTop + chartHeight * (1f - i.toFloat() / ySteps)
            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint)
            canvas.drawText(value.toInt().toString(), paddingLeft - 10f, y + 8f, yLabelPaint)
        }

        // Calculate point positions
        val stepX = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1) else chartWidth
        val points = dataPoints.mapIndexed { i, value ->
            val x = paddingLeft + i * stepX
            val y = paddingTop + chartHeight * (1f - (value - minValue) / range)
            Pair(x, y)
        }

        // Draw line path
        if (points.size > 1) {
            val path = Path()
            path.moveTo(points[0].first, points[0].second)
            for (i in 1 until points.size) {
                path.lineTo(points[i].first, points[i].second)
            }
            canvas.drawPath(path, linePaint)
        }

        // Draw dots and x-labels
        for (i in points.indices) {
            val paint = if (i == highlightIndex) highlightDotPaint else dotPaint
            val radius = if (i == highlightIndex) 10f else 7f
            canvas.drawCircle(points[i].first, points[i].second, radius, paint)

            if (i < labels.size) {
                canvas.save()
                canvas.rotate(-30f, points[i].first, height - 10f)
                canvas.drawText(labels[i], points[i].first, height - 10f, labelPaint)
                canvas.restore()
            }
        }
    }
}
