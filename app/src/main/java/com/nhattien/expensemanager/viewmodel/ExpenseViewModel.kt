package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class ExpenseViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).transactionDao()
    private val repository = TransactionRepository(dao)

    val expenses: Flow<List<TransactionEntity>> = repository.getAll()
}
