package com.nhattien.expensemanager.ui.chart

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

/**
 * Custom BarChart Renderer with rounded corners and gradient
 */
class RoundedBarChartRenderer(
    chart: BarDataProvider,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : BarChartRenderer(chart, animator, viewPortHandler) {

    private val roundedPath = Path()
    private var cornerRadius = 12f

    fun setCornerRadius(radius: Float) {
        this.cornerRadius = radius
    }

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val trans = mChart.getTransformer(dataSet.axisDependency)
        
        mBarBorderPaint.color = dataSet.barBorderColor
        mBarBorderPaint.strokeWidth = com.github.mikephil.charting.utils.Utils.convertDpToPixel(dataSet.barBorderWidth)

        val drawBorder = dataSet.barBorderWidth > 0f
        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        val buffer = mBarBuffers[index]
        buffer.setPhases(phaseX, phaseY)
        buffer.setDataSet(index)
        buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        buffer.setBarWidth(mChart.barData.barWidth)
        buffer.feed(dataSet)

        trans.pointValuesToPixel(buffer.buffer)

        val isSingleColor = dataSet.colors.size == 1

        if (isSingleColor) {
            mRenderPaint.color = dataSet.color
        }

        var j = 0
        while (j < buffer.size()) {
            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) {
                j += 4
                continue
            }
            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) {
                break
            }

            if (!isSingleColor) {
                mRenderPaint.color = dataSet.getColor(j / 4)
            }

            // Apply gradient
            val left = buffer.buffer[j]
            val top = buffer.buffer[j + 1]
            val right = buffer.buffer[j + 2]
            val bottom = buffer.buffer[j + 3]

            val gradient = LinearGradient(
                left, top, left, bottom,
                intArrayOf(
                    android.graphics.Color.parseColor("#FF6B6B"),  // Top - Light Red
                    android.graphics.Color.parseColor("#EE5A5A")   // Bottom - Darker Red
                ),
                null,
                Shader.TileMode.CLAMP
            )
            mRenderPaint.shader = gradient

            // Draw rounded rectangle
            roundedPath.reset()
            val rect = RectF(left, top, right, bottom)
            
            // Only round top corners
            val radii = floatArrayOf(
                cornerRadius, cornerRadius,  // Top-left
                cornerRadius, cornerRadius,  // Top-right
                0f, 0f,                      // Bottom-right
                0f, 0f                       // Bottom-left
            )
            roundedPath.addRoundRect(rect, radii, Path.Direction.CW)
            c.drawPath(roundedPath, mRenderPaint)

            // Reset shader for border
            mRenderPaint.shader = null

            if (drawBorder) {
                c.drawPath(roundedPath, mBarBorderPaint)
            }

            j += 4
        }
    }
}
