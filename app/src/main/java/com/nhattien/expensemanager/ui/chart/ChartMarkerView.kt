package com.nhattien.expensemanager.ui.chart

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.utils.CurrencyUtils

/**
 * Custom MarkerView (Tooltip) for charts
 * Shows formatted data when user touches a data point
 */
class ChartMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val txtContent: TextView? = findViewById(R.id.txtMarkerContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val day = it.x.toInt()
            val amount = it.y.toDouble()
            val formattedAmount = CurrencyUtils.toCurrency(amount)
            
            txtContent?.text = "Ngày $day\n$formattedAmount"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Center the marker horizontally and place above the point
        return MPPointF(-(width / 2f), -height.toFloat() - 10f)
    }
}
