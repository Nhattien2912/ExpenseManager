package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.repository.CategoryRepository
import com.nhattien.expensemanager.data.repository.ExpenseRepository

class WalletViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletViewModel::class.java)) {
            val db = AppDatabase.getInstance(application)
            val repository = ExpenseRepository(db.transactionDao(), db.debtDao(), db.tagDao(), db.walletDao(), db.searchHistoryDao())
            @Suppress("UNCHECKED_CAST")
            return WalletViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
