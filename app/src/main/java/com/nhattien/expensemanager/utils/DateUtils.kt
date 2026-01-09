package com.nhattien.expensemanager.utils

import com.nhattien.expensemanager.ui.model.CalendarDayUi
import java.util.Calendar

object DateUtils {

    // Hàm tạo danh sách ngày cho Lịch
    fun getDaysInMonth(month: Int, year: Int, dailyTotals: Map<Int, Double>): List<CalendarDayUi> {
        val list = ArrayList<CalendarDayUi>()
        val calendar = Calendar.getInstance()

        // 1. Setup ngày đầu tháng
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        // 2. Tính số ô trống đầu tháng
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val emptyDays = if (dayOfWeek == 1) 6 else dayOfWeek - 2

        for (i in 0 until emptyDays) {
            list.add(CalendarDayUi(0, "", 0.0, false))
        }

        // 3. Thêm các ngày thực tế
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()

        for (day in 1..maxDays) {
            val isToday = (day == today.get(Calendar.DAY_OF_MONTH) &&
                    month - 1 == today.get(Calendar.MONTH) &&
                    year == today.get(Calendar.YEAR))

            val dateStr = String.format("%d-%02d-%02d", year, month, day)
            val total = dailyTotals[day] ?: 0.0

            list.add(CalendarDayUi(day, dateStr, total, isToday))
        }
        return list
    }

    // Hàm lấy timestamp đầu ngày và cuối ngày
    fun getStartAndEndOfDay(dateStr: String): Pair<Long, Long> {
        val parts = dateStr.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()

        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(year, month - 1, day, 23, 59, 59)
        val end = calendar.timeInMillis
        return Pair(start, end)
    }
}