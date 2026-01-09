package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.repository.ExpenseRepository // [1] Import đúng

class AddTransactionViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTransactionViewModel::class.java)) {

            val db = AppDatabase.getInstance(application)
            // [2] Khởi tạo ExpenseRepository (cần cả 2 DAO)
            val repository = ExpenseRepository(db.transactionDao(), db.debtDao())

            @Suppress("UNCHECKED_CAST")
            return AddTransactionViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}