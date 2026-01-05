package com.nhattien.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.TransactionRepository
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    val transactions = repository.transactions

    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        note: String?
    ) {
        val transaction = TransactionEntity(
            amount = amount,
            type = type,
            category = category,
            note = note,
            date = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.insert(transaction)
        }
    }
}
