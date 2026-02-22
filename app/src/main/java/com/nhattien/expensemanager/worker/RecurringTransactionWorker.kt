package com.nhattien.expensemanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.utils.CurrencyUtils
import com.nhattien.expensemanager.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RecurringTransactionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getInstance(applicationContext)
            val recurringDao = database.recurringTransactionDao()
            val transactionDao = database.transactionDao()
            val walletDao = database.walletDao()

            val currentTime = System.currentTimeMillis()
            val dueTransactions = recurringDao.getDueTransactions(currentTime)

            var createdCount = 0

            for (recurring in dueTransactions) {
                var currentNextRunDate = recurring.nextRunDate
                var installmentsDone = recurring.completedInstallments
                
                // Keep generating transactions for missed periods until we catch up
                while (currentNextRunDate <= currentTime) {
                    // Check if installment limit reached (0 = unlimited)
                    if (recurring.totalInstallments > 0 && installmentsDone >= recurring.totalInstallments) {
                        break
                    }
                    
                    // 1. Create actual transaction
                    val newTransaction = TransactionEntity(
                        amount = recurring.amount,
                        categoryId = recurring.categoryId,
                        type = recurring.type,
                        note = recurring.note,
                        date = currentNextRunDate,
                        isRecurring = true,
                        walletId = recurring.walletId
                    )
                    transactionDao.insertTransaction(newTransaction)

                    // 2. Update wallet balance
                    val wallet = walletDao.getWalletById(recurring.walletId)
                    if (wallet != null) {
                        val newBalance = if (recurring.type == com.nhattien.expensemanager.domain.TransactionType.INCOME) {
                            wallet.initialBalance + recurring.amount
                        } else {
                            wallet.initialBalance - recurring.amount
                        }
                        walletDao.updateWallet(wallet.copy(initialBalance = newBalance))
                    }

                    // 3. Advance to next period
                    currentNextRunDate = calculateNextRunDate(currentNextRunDate, recurring.recurrencePeriod)
                    installmentsDone++
                    createdCount++
                }
                
                // 4. Update recurring entity
                val shouldDeactivate = recurring.totalInstallments > 0 && installmentsDone >= recurring.totalInstallments
                recurringDao.update(
                    recurring.copy(
                        nextRunDate = currentNextRunDate,
                        completedInstallments = installmentsDone,
                        isActive = if (shouldDeactivate) false else recurring.isActive
                    )
                )
                
                if (shouldDeactivate) {
                    val sourceLabel = if (recurring.loanSource == "BANK") "Ngân hàng" else "Cá nhân"
                    NotificationHelper.showNotification(
                        applicationContext,
                        "Hoàn tất trả nợ!",
                        "Khoản nợ $sourceLabel ${CurrencyUtils.toCurrency(recurring.amount)}/kỳ đã trả đủ ${recurring.totalInstallments} kỳ."
                    )
                }
            }

            if (createdCount > 0) {
                NotificationHelper.showNotification(
                    applicationContext,
                    "Giao dịch định kỳ",
                    "Đã tự động tạo $createdCount giao dịch đến hạn. Cập nhật số dư ví."
                )
            }
            
            // --- REMINDER: Check for transactions due within next 24 hours ---
            val tomorrow = currentTime + 24 * 60 * 60 * 1000L
            val upcomingTransactions = recurringDao.getUpcomingTransactions(currentTime, tomorrow)
            
            for (upcoming in upcomingTransactions) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dateStr = sdf.format(upcoming.nextRunDate)
                val sourceLabel = if (upcoming.loanSource == "BANK") "🏦 Ngân hàng" else "👤 Cá nhân"
                val progressText = if (upcoming.totalInstallments > 0) {
                    " (Kỳ ${upcoming.completedInstallments + 1}/${upcoming.totalInstallments})"
                } else ""
                
                NotificationHelper.showNotification(
                    applicationContext,
                    "Nhắc nhở thanh toán $sourceLabel",
                    "Ngày $dateStr cần thanh toán ${CurrencyUtils.toCurrency(upcoming.amount)}$progressText"
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun calculateNextRunDate(currentNextRun: Long, period: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentNextRun
        }
        
        when (period) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
        }
        
        return calendar.timeInMillis
    }
}
