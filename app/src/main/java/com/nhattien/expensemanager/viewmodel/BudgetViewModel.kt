package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository // [1] Dùng cái này
import com.nhattien.expensemanager.domain.Category
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    // [2] Khởi tạo ExpenseRepository chuẩn
    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
    }

    // [3] Lấy dữ liệu từ ExpenseRepository
    private val allTransactions = repository.allTransactions

    // --- CÁC PHẦN DƯỚI GIỮ NGUYÊN ---

    // 1. Tính tổng PHẢI THU
    val totalReceivable = allTransactions.map { list ->
        val given = list.filter { it.category == Category.LENDING }.sumOf { it.amount }
        val collected = list.filter { it.category == Category.DEBT_COLLECTION }.sumOf { it.amount }
        given - collected
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 2. Tính tổng PHẢI TRẢ
    val totalPayable = allTransactions.map { list ->
        val borrowed = list.filter { it.category == Category.BORROWING }.sumOf { it.amount }
        val repaid = list.filter { it.category == Category.DEBT_REPAYMENT }.sumOf { it.amount }
        borrowed - repaid
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 3. Danh sách Nợ
    val debtTransactions = allTransactions.map { list ->
        list.filter {
            it.category == Category.LENDING ||
                    it.category == Category.BORROWING ||
                    it.category == Category.DEBT_COLLECTION ||
                    it.category == Category.DEBT_REPAYMENT
        }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 4. Gạch Nợ
    fun settleDebt(origin: TransactionEntity) {
        viewModelScope.launch {
            val (newType, newCategory, notePrefix) = when (origin.category) {
                Category.LENDING -> Triple(TransactionType.INCOME, Category.DEBT_COLLECTION, "Đã thu nợ khoản")
                Category.BORROWING -> Triple(TransactionType.EXPENSE, Category.DEBT_REPAYMENT, "Đã trả nợ khoản")
                else -> return@launch
            }

            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(origin.date))

            val settleTransaction = TransactionEntity(
                amount = origin.amount,
                type = newType,
                category = newCategory,
                note = "$notePrefix $dateStr",
                date = System.currentTimeMillis()
            )
            // Gọi hàm insertTransaction chuẩn
            repository.insertTransaction(settleTransaction)
        }
    }
}