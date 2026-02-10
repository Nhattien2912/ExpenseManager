package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao(), db.tagDao(), db.walletDao())
    }

    // Lấy các giao dịch liên quan đến tiết kiệm (Gửi tiết kiệm / Rút tiết kiệm)
    val savingTransactions: StateFlow<List<TransactionWithCategory>> = repository.allTransactions
        .map { list ->
            list.filter { 
                it.category.name == "Gửi tiết kiệm" || it.category.name == "Rút tiết kiệm"
            }.sortedByDescending { it.transaction.date }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Tính tổng tiền tiết kiệm (Gửi - Rút)
    val totalSavings: StateFlow<Double> = savingTransactions
        .map { list ->
            val deposited = list.filter { it.category.name == "Gửi tiết kiệm" }
                .sumOf { it.transaction.amount }
            val withdrawn = list.filter { it.category.name == "Rút tiết kiệm" }
                .sumOf { it.transaction.amount }
            deposited - withdrawn
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)
}
