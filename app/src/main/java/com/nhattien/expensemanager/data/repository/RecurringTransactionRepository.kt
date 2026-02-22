package com.nhattien.expensemanager.data.repository

import com.nhattien.expensemanager.data.dao.RecurringTransactionDao
import com.nhattien.expensemanager.data.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

class RecurringTransactionRepository(
    private val recurringDao: RecurringTransactionDao
) {
    val allRecurring: Flow<List<RecurringTransactionEntity>> = recurringDao.getAllRecurring()

    suspend fun getDueTransactions(currentTime: Long): List<RecurringTransactionEntity> {
        return recurringDao.getDueTransactions(currentTime)
    }

    suspend fun insert(transaction: RecurringTransactionEntity): Long {
        return recurringDao.insert(transaction)
    }

    suspend fun update(transaction: RecurringTransactionEntity) {
        recurringDao.update(transaction)
    }

    suspend fun delete(transaction: RecurringTransactionEntity) {
        recurringDao.delete(transaction)
    }

    suspend fun updateStatus(id: Long, isActive: Boolean) {
        recurringDao.updateStatus(id, isActive)
    }
}
