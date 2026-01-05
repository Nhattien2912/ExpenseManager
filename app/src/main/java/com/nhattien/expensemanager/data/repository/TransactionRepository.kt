package com.nhattien.expensemanager.data.repository

import com.nhattien.expensemanager.data.dao.TransactionDao
import com.nhattien.expensemanager.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val dao: TransactionDao
) {
    val transactions = dao.getAll()
    fun getAll(): Flow<List<TransactionEntity>> = dao.getAll()

    suspend fun insert(transaction: TransactionEntity) {
        dao.insert(transaction)
    }
}
