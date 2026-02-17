package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.DebtEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

class AddDebtViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao(), db.tagDao(), db.walletDao(), db.searchHistoryDao())
    }

    fun addDebt(
        name: String,
        amount: Double,
        isMeLending: Boolean,
        dueDate: Long?,
        interest: Double
    ) {
        val entity = DebtEntity(
            debtorName = name,
            amount = amount,
            isMeLending = isMeLending,
            startDate = System.currentTimeMillis(),
            dueDate = dueDate,
            interestRate = interest,
            isFinished = false
        )
        viewModelScope.launch {
            repository.insertDebt(entity)
        }
    }
}