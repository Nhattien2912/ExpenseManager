package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
    }

    // Lấy tất cả giao dịch (để tính toán)
    private val allTransactions = repository.allTransactions

    // 1. LIST GIAO DỊCH GẦN ĐÂY (Lấy 10 cái mới nhất)
    val recentTransactions = allTransactions.map { list ->
        list.take(10)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 2. TỔNG TÀI SẢN (Total Balance)
    val totalBalance = allTransactions.map { list ->
        list.sumOf { calculateSignedAmount(it) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 3. BIẾN ĐỘNG HÔM NAY (Today)
    val todayBalance = allTransactions.map { list ->
        val todayStart = getStartOfDay()
        val todayEnd = getEndOfDay()
        list.filter { it.date in todayStart..todayEnd }
            .sumOf { calculateSignedAmount(it) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 4. BIẾN ĐỘNG THÁNG NAY (Month)
    val monthBalance = allTransactions.map { list ->
        val (monthStart, monthEnd) = getMonthRange()
        list.filter { it.date in monthStart..monthEnd }
            .sumOf { calculateSignedAmount(it) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)


    // === HÀM BỔ TRỢ ===

    // Quy định: Thu/Đi vay = Dương (+), Chi/Cho vay = Âm (-)
    private fun calculateSignedAmount(item: TransactionEntity): Double {
        return when (item.type) {
            TransactionType.INCOME, TransactionType.LOAN_TAKE -> item.amount
            TransactionType.EXPENSE, TransactionType.LOAN_GIVE -> -item.amount
        }
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}