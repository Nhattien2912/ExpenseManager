package com.nhattien.expensemanager.ui.daydetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DayDetailViewModel (
    application: Application,
    private val startOfDay: Long,
    private val endOfDay: Long
) : AndroidViewModel(application) {

    private val dao = AppDatabase
        .getInstance(application)
        .transactionDao()

    val transactions: Flow<List<TransactionWithCategory>> =
        dao.getTransactionsInRange(startOfDay, endOfDay)

    val totalIncome: Flow<Double> =
        transactions.map { list ->
            list.filter { it.transaction.type == TransactionType.INCOME }
                .sumOf { it.transaction.amount }
        }

    val totalExpense: Flow<Double> =
        transactions.map { list ->
            list.filter { it.transaction.type == TransactionType.EXPENSE }
                .sumOf { it.transaction.amount }
        }
}