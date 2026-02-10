package com.nhattien.expensemanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.domain.TransactionType
import com.nhattien.expensemanager.utils.NotificationHelper
import java.util.Calendar

class BudgetCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("expense_manager", Context.MODE_PRIVATE)
            
            // Check nếu user tắt tính năng này
            val isBudgetAlertEnabled = prefs.getBoolean("KEY_BUDGET_ALERT", true)
            if (!isBudgetAlertEnabled) {
                return Result.success()
            }

            // Lấy limit - Note: ViewModels save as Float, not Long
            val limitAmount = prefs.getFloat("KEY_SPENDING_LIMIT", 5000000f).toDouble()
            if (limitAmount <= 0) {
                return Result.success()
            }

            // Tính tổng chi tiêu tháng này
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1) // Ngày đầu tháng
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, -1) // Ngày cuối tháng
            val endOfMonth = calendar.timeInMillis

            val database = AppDatabase.getInstance(applicationContext)
            val transactions = database.transactionDao().getTransactionsInRangeSync(startOfMonth, endOfMonth)

// ... inside doWork
            var totalExpense = 0.0
            for (tx in transactions) {
                if (tx.type == TransactionType.EXPENSE) {
                    totalExpense += tx.amount
                }
            }

            // Tính tỷ lệ
            val percentage = (totalExpense / limitAmount * 100).toInt()

            // Logic cảnh báo
            // Chỉ cảnh báo nếu vượt 80% hoặc 100%
            // Để tránh spam, có thể lưu lại trạng thái "đã cảnh báo tháng này" vào prefs
            // Nhưng hiện tại làm đơn giản trước.
            
            if (percentage >= 100) {
                 NotificationHelper.showBudgetWarning(applicationContext, percentage)
            } else if (percentage >= 80) {
                 // Check if already warned for 80%? skip for now to keep simple
                 NotificationHelper.showBudgetWarning(applicationContext, percentage)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
