package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.data.repository.CategoryRepository
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val categoryRepository: CategoryRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
        categoryRepository = CategoryRepository(db.categoryDao())
    }

    private val allTransactions = repository.allTransactions

    // 1. Tính tổng PHẢI THU (Receivable)
    val totalReceivable = allTransactions.map { list ->
        // Cho vay (LOAN_GIVE) - Thu nợ (LOAN_TAKE)
        // Note: Using TransactionType to distinguish. 
        // LOAN_GIVE covers Lending and Repayment(of debt to us? No, Repayment of our debt).
        // Let's refine by Category Name for now to match legacy logic 100%.
        
        val given = list.filter { 
            it.transaction.type == TransactionType.LOAN_GIVE && it.category.name == "Cho vay" 
        }.sumOf { it.transaction.amount }
        
        val collected = list.filter { 
            it.transaction.type == TransactionType.LOAN_TAKE && it.category.name == "Thu nợ" 
        }.sumOf { it.transaction.amount }
        
        given - collected
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 2. Tính tổng PHẢI TRẢ (Payable)
    val totalPayable = allTransactions.map { list ->
        // Đi vay (LOAN_TAKE) - Trả nợ (LOAN_GIVE)
        val borrowed = list.filter { 
            it.transaction.type == TransactionType.LOAN_TAKE && it.category.name == "Đi vay" 
        }.sumOf { it.transaction.amount }
        
        val repaid = list.filter { 
            it.transaction.type == TransactionType.LOAN_GIVE && it.category.name == "Trả nợ" 
        }.sumOf { it.transaction.amount }
        
        borrowed - repaid
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 3. Danh sách Nợ
    val debtTransactions = allTransactions.map { list ->
        list.filter {
            it.transaction.type == TransactionType.LOAN_GIVE || it.transaction.type == TransactionType.LOAN_TAKE
        }.sortedByDescending { it.transaction.date }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 4. Gạch Nợ
    fun settleDebt(origin: TransactionEntity) {
        viewModelScope.launch {
            // Logic:
            // IF LOAN_GIVE (Cho vay) -> Settle by Collecting (Thu nợ - LOAN_TAKE)
            // IF LOAN_TAKE (Đi vay) -> Settle by Repaying (Trả nợ - LOAN_GIVE)
            
            val targetName = if (origin.type == TransactionType.LOAN_GIVE) "Thu nợ" else "Trả nợ"
            val targetCategory = categoryRepository.getCategoryByName(targetName) ?: return@launch

            val (newType, notePrefix) = if (origin.type == TransactionType.LOAN_GIVE) {
                 Pair(TransactionType.LOAN_TAKE, "Đã thu nợ khoản")
            } else {
                 Pair(TransactionType.LOAN_GIVE, "Đã trả nợ khoản") // Repaying debt is Money Out (LOAN_GIVE concept used for money leaving wallet)
            }

            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(origin.date))

            val settleTransaction = TransactionEntity(
                amount = origin.amount,
                type = newType,
                categoryId = targetCategory.id,
                note = "$notePrefix $dateStr",
                date = System.currentTimeMillis()
            )
            repository.insertTransaction(settleTransaction)
        }
    }
    

    // --- Spending Limit Logic ---
    private val prefs = application.getSharedPreferences("expense_manager", android.content.Context.MODE_PRIVATE)
    private val _spendingLimit = kotlinx.coroutines.flow.MutableStateFlow(prefs.getFloat("KEY_SPENDING_LIMIT", 5000000f).toDouble()) 
    val spendingLimit = _spendingLimit
    
    // Listener to sync spending limit from other ViewModels
    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "KEY_SPENDING_LIMIT") {
            _spendingLimit.value = prefs.getFloat("KEY_SPENDING_LIMIT", 5000000f).toDouble()
        }
    }
    
    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }
    
    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    val currentMonthExpense = allTransactions.map { list ->
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        list.filter {
            it.transaction.type == TransactionType.EXPENSE &&
            convertDateToCalendar(it.transaction.date).let { cal ->
                cal.get(java.util.Calendar.MONTH) == currentMonth &&
                cal.get(java.util.Calendar.YEAR) == currentYear
            }
        }.sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    fun setSpendingLimit(amount: Double) {
        prefs.edit().putFloat("KEY_SPENDING_LIMIT", amount.toFloat()).apply()
        _spendingLimit.value = amount
    }

    private fun convertDateToCalendar(timestamp: Long): java.util.Calendar {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal
    }
}