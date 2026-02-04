---
name: ExpenseManager Calendar
description: Cách làm việc với Calendar trong ExpenseManager - hiển thị ngày, tháng, và daily sums.
---

# Calendar trong ExpenseManager

## DateUtils

### Lấy danh sách ngày trong tháng
```kotlin
import com.nhattien.expensemanager.utils.DateUtils

val days = DateUtils.getDaysInMonth(
    month = 1,  // January (1-12)
    year = 2024,
    dailyTotals = viewModel.calendarDailyTotals.value
)
// Returns: List<CalendarDayUi>
```

### Lấy start/end timestamp của ngày
```kotlin
val (startOfDay, endOfDay) = DateUtils.getStartAndEndOfDay("2024-01-15")
// startOfDay: 00:00:00
// endOfDay: 23:59:59
```

## CalendarDayUi Model

```kotlin
data class CalendarDayUi(
    val day: Int,           // 0 = empty cell, 1-31 = actual day
    val dateStr: String,    // "2024-01-15" format
    val income: Double,     // Thu nhập trong ngày
    val expense: Double,    // Chi tiêu trong ngày
    val isToday: Boolean    // Highlight ngày hôm nay
)
```

## DailySum Model

```kotlin
data class DailySum(
    val income: Double = 0.0,
    val expense: Double = 0.0
)
```

## Tính toán Daily Totals (MainViewModel)

```kotlin
val calendarDailyTotals = monthlyStats.map { (_, _, list) ->
    val map = mutableMapOf<Int, DailySum>()
    list.forEach { 
        val day = Calendar.getInstance().apply { 
            timeInMillis = it.transaction.date 
        }.get(Calendar.DAY_OF_MONTH)
        
        val current = map[day] ?: DailySum()
        if (it.transaction.type == TransactionType.INCOME) {
            map[day] = current.copy(income = current.income + it.amount)
        } else {
            map[day] = current.copy(expense = current.expense + it.amount)
        }
    }
    map
}.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
```

## Thay đổi tháng

```kotlin
// Trong ViewModel
fun changeMonth(offset: Int) {
    val newCal = Calendar.getInstance().apply { 
        timeInMillis = currentCalendar.value.timeInMillis
        add(Calendar.MONTH, offset) 
    }
    currentCalendar.value = newCal
}

// Đặt tháng cụ thể
fun setCurrentMonth(year: Int, month: Int) {
    val newCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)  // 0-indexed
    }
    currentCalendar.value = newCal
}
```

## CalendarAdapter hiển thị

```kotlin
fun bind(day: CalendarDayUi) {
    if (day.day == 0) {
        // Empty cell
        itemView.visibility = View.INVISIBLE
        return
    }
    
    txtDay.text = day.day.toString()
    
    if (day.income > 0) {
        txtIncome.text = "+${CurrencyUtils.formatShort(day.income)}"
        txtIncome.visibility = View.VISIBLE
    }
    
    if (day.expense > 0) {
        txtExpense.text = "-${CurrencyUtils.formatShort(day.expense)}"
        txtExpense.visibility = View.VISIBLE
    }
    
    if (day.isToday) {
        itemView.setBackgroundResource(R.drawable.bg_today)
    }
}
```
