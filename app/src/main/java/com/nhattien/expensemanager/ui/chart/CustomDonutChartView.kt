package com.nhattien.expensemanager.ui.chart

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

data class DonutChartData(
    val name: String,
    val value: Float,
    val color: Int,
    val icon: String
)

class CustomDonutChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dataList: List<DonutChartData> = emptyList()
    private var totalValue = 0f
    private var animateProgress = 0f
    
    // Paints
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f // dp
        pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
    }
    
    private val textPaintPrimary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val textPaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    
    private val centerTextPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
    }
    
    private val centerTextPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val rectF = RectF()
    private val path = Path()

    // Configs
    private var strokeW = 100f // Will be scaled
    private var isDarkMode = false
    private var totalText = "0 đ"

    fun setData(data: List<DonutChartData>, totalTextFormatted: String) {
        this.dataList = data
        this.totalValue = data.sumOf { it.value.toDouble() }.toFloat()
        this.totalText = totalTextFormatted
        
        val prefs = context.getSharedPreferences("expense_manager", Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean("KEY_DARK_MODE", false)
        
        textPaintPrimary.color = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#333333")
        textPaintSecondary.color = if (isDarkMode) Color.parseColor("#A0A0A0") else Color.parseColor("#757575")
        centerTextPaint1.color = if (isDarkMode) Color.parseColor("#A0A0A0") else Color.parseColor("#757575")
        centerTextPaint2.color = if (isDarkMode) Color.parseColor("#E0E0E0") else Color.parseColor("#333333")

        startAnimation()
    }

    private fun startAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animateProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataList.isEmpty() || totalValue == 0f) return

        val cx = width / 2f
        val cy = height / 2f
        
        val density = resources.displayMetrics.density
        strokeW = 40f * density
        textPaintPrimary.textSize = 13f * density
        textPaintSecondary.textSize = 10f * density
        centerTextPaint1.textSize = 12f * density
        centerTextPaint2.textSize = 18f * density
        linePaint.strokeWidth = 1.5f * density

        // Radius of the circle itself
        val r = (width.coerceAtMost(height) / 2f) - (strokeW / 2f) - (65f * density)
        rectF.set(cx - r, cy - r, cx + r, cy + r)
        arcPaint.strokeWidth = strokeW

        var startAngle = 270f // Top
        
        // Draw Center Text
        canvas.drawText("Tổng chi", cx, cy - (8f * density), centerTextPaint1)
        canvas.drawText(totalText, cx, cy + (16f * density), centerTextPaint2)

        for (item in dataList) {
            val sweepAngle = (item.value / totalValue) * 360f * animateProgress
            val pctDisplay = Math.round((item.value / totalValue) * 100)
            
            // Draw slice
            arcPaint.color = item.color
            // Add a tiny gap between slices
            val actualSweep = if (dataList.size > 1 && sweepAngle > 2f) sweepAngle - 2f else sweepAngle
            canvas.drawArc(rectF, startAngle, actualSweep, false, arcPaint)

            // Draw line and labels if slice is big enough and we're at end of animation (or nearing)
            if (pctDisplay >= 3 && animateProgress > 0.8f) {
                val alpha = ((animateProgress - 0.8f) * 5 * 255).toInt().coerceIn(0, 255)
                linePaint.alpha = (alpha * 0.7f).toInt()
                textPaintPrimary.alpha = alpha
                textPaintSecondary.alpha = alpha
                
                val midAngle = startAngle + (sweepAngle / 2f)
                val midRad = Math.toRadians(midAngle.toDouble())

                // Line start (just outside donut)
                val lineStartR = r + (strokeW / 2f) + (4f * density)
                val lineStartX = cx + (lineStartR * cos(midRad)).toFloat()
                val lineStartY = cy + (lineStartR * sin(midRad)).toFloat()
                
                // Line end
                val lineEndR = r + (strokeW / 2f) + (30f * density)
                val lineEndX = cx + (lineEndR * cos(midRad)).toFloat()
                val lineEndY = cy + (lineEndR * sin(midRad)).toFloat()

                linePaint.color = item.color
                path.reset()
                path.moveTo(lineStartX, lineStartY)
                path.lineTo(lineEndX, lineEndY)
                canvas.drawPath(path, linePaint)

                // Label Position
                val labelR = r + (strokeW / 2f) + (45f * density)
                val labelX = cx + (labelR * cos(midRad)).toFloat()
                val labelY = cy + (labelR * sin(midRad)).toFloat()

                val primaryText = "${item.icon} $pctDisplay%"
                val secondaryText = if (item.name.length > 12) item.name.take(12) + "…" else item.name

                canvas.drawText(primaryText, labelX, labelY - (6f * density) + (textPaintPrimary.textSize / 3), textPaintPrimary)
                canvas.drawText(secondaryText, labelX, labelY + (10f * density) + (textPaintSecondary.textSize / 3), textPaintSecondary)
            }

            startAngle += sweepAngle
        }
    }
}
