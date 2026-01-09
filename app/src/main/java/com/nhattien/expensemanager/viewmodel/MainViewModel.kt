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
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
    }

    val allTransactions = repository.allTransactions

    // 1. DASHBOARD: List giao dịch gần đây (10 cái)
    val recentTransactions = allTransactions.map { list ->
        list.take(10)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 2. DASHBOARD: Tổng tài sản
    val totalBalance = allTransactions.map { list ->
        list.sumOf { calculateSignedAmount(it) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 3. DASHBOARD: Thu / Chi tháng này
    val monthlyIncome = allTransactions.map { list ->
        val (start, end) = getMonthRange()
        list.filter { it.date in start..end && (it.type == TransactionType.INCOME || it.type == TransactionType.LOAN_TAKE) }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val monthlyExpense = allTransactions.map { list ->
        val (start, end) = getMonthRange()
        list.filter { it.date in start..end && (it.type == TransactionType.EXPENSE || it.type == TransactionType.LOAN_GIVE) }
            .sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 4. LỊCH: Tính tổng tiền từng ngày (Để hiện lên ô lịch)
    // Trả về Map: Key là ngày (Int), Value là tổng tiền (Double)
    val calendarDailyTotals = allTransactions.map { list ->
        val map = mutableMapOf<Int, Double>()
        list.forEach { item ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = item.date
            val day = cal.get(Calendar.DAY_OF_MONTH)
            // Chỉ tính cho tháng/năm hiện tại (Ở đây làm đơn giản, đúng ra phải lọc theo tháng đang xem)
            // Để đơn giản cho demo, ta cứ cộng dồn vào ngày tương ứng
            val currentAmount = map.getOrDefault(day, 0.0)
            map[day] = currentAmount + calculateSignedAmount(item)
        }
        map
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // 5. CHỨC NĂNG: Xóa giao dịch
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // === HÀM PHỤ ===
    private fun calculateSignedAmount(item: TransactionEntity): Double {
        return when (item.type) {
            TransactionType.INCOME, TransactionType.LOAN_TAKE -> item.amount
            TransactionType.EXPENSE, TransactionType.LOAN_GIVE -> -item.amount
        }
    }

    private fun getMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis
        return Pair(start, end)
    }
}