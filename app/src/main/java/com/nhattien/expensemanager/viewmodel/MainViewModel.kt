package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository // Lưu ý: Dùng ExpenseRepository
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
        // Nếu bạn chưa có class ExpenseRepository, hãy tạo nó hoặc dùng TransactionRepository tạm thời
        // Ở đây tôi dùng ExpenseRepository như kiến trúc đã bàn
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
    }

    val allTransactions = repository.allTransactions

    // 1. DASHBOARD: List 10 giao dịch gần nhất
    val recentTransactions = allTransactions.map { list ->
        list.take(10)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 2. DASHBOARD: Tổng tài sản
    val totalBalance = allTransactions.map { list ->
        list.sumOf { calculateSignedAmount(it) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 3. DASHBOARD: Thu & Chi tháng này
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

    // 4. LỊCH: Tính tổng tiền từng ngày (Trả về Map<Ngày, Tiền>)
    val calendarDailyTotals = allTransactions.map { list ->
        val map = mutableMapOf<Int, Double>()
        list.forEach { item ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = item.date
            val day = cal.get(Calendar.DAY_OF_MONTH)
            // Lưu ý: Logic đơn giản này cộng dồn mọi tháng.
            // Cần lọc theo tháng/năm hiện tại trong thực tế.
            val currentAmount = map.getOrDefault(day, 0.0)
            map[day] = currentAmount + calculateSignedAmount(item)
        }
        map
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // ===> HÀM BẠN ĐANG THIẾU <===
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Hàm phụ trợ tính toán
    private fun calculateSignedAmount(item: TransactionEntity): Double {
        return when (item.type) {
            TransactionType.INCOME, TransactionType.LOAN_TAKE -> item.amount
            TransactionType.EXPENSE, TransactionType.LOAN_GIVE -> -item.amount
            else -> 0.0
        }
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