package com.nhattien.expensemanager.ui.model

data class CalendarDayUi(
    val day: Int,
    val date: String,
    val income: Double = 0.0,  // Tiền thu
    val expense: Double = 0.0, // Tiền chi
    val isToday: Boolean = false
)
