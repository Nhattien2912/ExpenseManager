---
name: ExpenseManager Charts
description: Sử dụng MPAndroidChart để hiển thị biểu đồ trong ExpenseManager.
---

# Charts trong ExpenseManager

## Dependencies
```groovy
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
```

## Pie Chart (Phân bổ theo Category)

```kotlin
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

private fun setupPieChart(distribution: Map<CategoryEntity, Double>) {
    val entries = distribution.map { (category, percent) ->
        PieEntry(percent.toFloat(), category.name)
    }
    
    val dataSet = PieDataSet(entries, "Chi tiêu").apply {
        colors = listOf(
            Color.parseColor("#FF6384"),
            Color.parseColor("#36A2EB"),
            Color.parseColor("#FFCE56"),
            Color.parseColor("#4BC0C0")
        )
        valueTextSize = 12f
        valueTextColor = Color.WHITE
    }
    
    binding.pieChart.apply {
        data = PieData(dataSet)
        description.isEnabled = false
        legend.isEnabled = true
        setUsePercentValues(true)
        animateY(1000)
        invalidate()
    }
}
```

## Bar Chart (Chi tiêu theo ngày)

```kotlin
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

private fun setupBarChart(dailyExpense: Map<Int, Double>) {
    val entries = dailyExpense.map { (day, amount) ->
        BarEntry(day.toFloat(), amount.toFloat())
    }
    
    val dataSet = BarDataSet(entries, "Chi tiêu hàng ngày").apply {
        color = Color.parseColor("#F44336")
        valueTextSize = 10f
    }
    
    binding.barChart.apply {
        data = BarData(dataSet)
        description.isEnabled = false
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        axisLeft.axisMinimum = 0f
        axisRight.isEnabled = false
        animateY(1000)
        invalidate()
    }
}
```

## Line Chart (Biến động số dư)

```kotlin
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry

private fun setupLineChart(balanceTrend: List<Pair<Int, Double>>) {
    val entries = balanceTrend.map { (day, balance) ->
        Entry(day.toFloat(), balance.toFloat())
    }
    
    val dataSet = LineDataSet(entries, "Số dư").apply {
        color = Color.parseColor("#4CAF50")
        lineWidth = 2f
        setDrawCircles(false)
        setDrawFilled(true)
        fillColor = Color.parseColor("#4CAF50")
        fillAlpha = 50
        mode = LineDataSet.Mode.CUBIC_BEZIER
    }
    
    binding.lineChart.apply {
        data = LineData(dataSet)
        description.isEnabled = false
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        animateX(1000)
        invalidate()
    }
}
```

## ChartType trong MainViewModel

```kotlin
enum class ChartType { PIE, BAR, LINE }

private val _chartType = MutableStateFlow(ChartType.PIE)
val chartType = _chartType

fun setChartType(type: ChartType) {
    _chartType.value = type
}
```

## Observe và hiển thị

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    combine(
        viewModel.chartType,
        viewModel.categoryDistribution,
        viewModel.dailyExpenseData,
        viewModel.balanceTrendData
    ) { type, pie, bar, line ->
        when (type) {
            ChartType.PIE -> setupPieChart(pie)
            ChartType.BAR -> setupBarChart(bar)
            ChartType.LINE -> setupLineChart(line)
        }
    }.collect()
}
```
