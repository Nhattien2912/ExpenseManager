package com.nhattien.expensemanager.ui.model

data class CalendarDayUi(
    val day: Int,          // số ngày: 1..31
    val date: String,      // yyyy-MM-dd (để mở DayDetail)
    val total: Double,     // tổng tiền ngày đó
    val isToday: Boolean   // có phải hôm nay không
)
