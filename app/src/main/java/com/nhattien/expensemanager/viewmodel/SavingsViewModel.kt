package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.domain.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
    }

    private val allTransactions = repository.allTransactions

    // 1. Saving History (Gửi vào + Rút ra)
    val savingTransactions = allTransactions.map { list ->
        list.filter {
            it.category == Category.SAVING_IN || it.category == Category.SAVING_OUT
        }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 2. Total Savings Balance
    val totalSavings = savingTransactions.map { list ->
        val deposited = list.filter { it.category == Category.SAVING_IN }.sumOf { it.amount }
        val withdrawn = list.filter { it.category == Category.SAVING_OUT }.sumOf { it.amount }
        deposited - withdrawn
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)
    
}
