package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.data.repository.CategoryRepository
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enum for Debt Tabs
enum class DebtTab { RECEIVABLE, PAYABLE }

class DebtViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val categoryRepository: CategoryRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
        categoryRepository = CategoryRepository(db.categoryDao())
    }

    private val allTransactions = repository.allTransactions

    // --- Tab Selection ---
    private val _currentTab = MutableStateFlow(DebtTab.RECEIVABLE)
    val currentTab = _currentTab

    fun setTab(tab: DebtTab) {
        _currentTab.value = tab
    }

    // --- Data Streams ---
    
    // 1. Receivable (Cho vay) Logic
    val receivableTransactions = allTransactions.map { list ->
        list.filter { it.transaction.type == TransactionType.LOAN_GIVE && it.category.name == "Cho vay" }
            .sortedByDescending { it.transaction.date }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalReceivable = receivableTransactions.map { list ->
        list.sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 2. Payable (Đi vay) Logic
    val payableTransactions = allTransactions.map { list ->
        list.filter { it.transaction.type == TransactionType.LOAN_TAKE && it.category.name == "Đi vay" }
            .sortedByDescending { it.transaction.date }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalPayable = payableTransactions.map { list ->
        list.sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // 3. Current List based on Tab
    val currentList = combine(_currentTab, receivableTransactions, payableTransactions) { tab, receivable, payable ->
        if (tab == DebtTab.RECEIVABLE) receivable else payable
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 4. Settle Debt (Mark as Paid/Received)
    fun settleDebt(origin: TransactionEntity) {
        viewModelScope.launch {
            val targetName = if (origin.type == TransactionType.LOAN_GIVE) "Thu nợ" else "Trả nợ"
            val targetCategory = categoryRepository.getCategoryByName(targetName) ?: return@launch

            val (newType, notePrefix) = if (origin.type == TransactionType.LOAN_GIVE) {
                 Pair(TransactionType.LOAN_TAKE, "Thu nợ từ giao dịch ngày")
            } else {
                 Pair(TransactionType.LOAN_GIVE, "Trả nợ cho giao dịch ngày")
            }

            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(origin.date))
            
            val settleTransaction = TransactionEntity(
                amount = origin.amount,
                type = newType,
                categoryId = targetCategory.id,
                note = "$notePrefix $dateStr: ${origin.note}",
                date = System.currentTimeMillis()
            )
            repository.insertTransaction(settleTransaction)
            
            // Delete original logic from previous file? 
            // In the previous code I saw a deleteTransaction call.
            // Let's keep it to clear the list.
            repository.deleteTransaction(origin)
        }
    }
}
