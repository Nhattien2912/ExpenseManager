package com.nhattien.expensemanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.utils.CurrencyUtils
import com.nhattien.expensemanager.utils.NotificationHelper
import java.util.Calendar

class DebtReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val debts = database.debtDao().getUnfinishedDebtsList()

            val calendar = Calendar.getInstance()
            
            // Xóa giờ phút giây để chỉ so sánh ngày
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val threeDaysLater = Calendar.getInstance().apply {
                timeInMillis = today.timeInMillis
                add(Calendar.DAY_OF_YEAR, 3)
            }

            for (debt in debts) {
                // Chỉ nhắc nếu có due date
                if (debt.dueDate != null && debt.dueDate > 0) {
                    val dueDateCal = Calendar.getInstance().apply {
                        timeInMillis = debt.dueDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    // Nếu due date là hôm nay hoặc trong vòng 3 ngày tới
                    if (dueDateCal.timeInMillis >= today.timeInMillis && 
                        dueDateCal.timeInMillis <= threeDaysLater.timeInMillis) {
                        
                        NotificationHelper.showDebtReminder(
                            applicationContext,
                            debt.id,
                            if (debt.isMeLending) "Khách nợ: ${debt.debtorName}" else "Nợ: ${debt.debtorName}",
                            CurrencyUtils.toCurrency(debt.amount)
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
