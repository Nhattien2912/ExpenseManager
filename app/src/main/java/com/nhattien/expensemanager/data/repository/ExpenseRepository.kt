package com.nhattien.expensemanager.data.repository

import com.nhattien.expensemanager.data.dao.DebtDao
import com.nhattien.expensemanager.data.dao.TransactionDao
import com.nhattien.expensemanager.data.entity.DebtEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.domain.TransactionType // [SỬA LẠI DÒNG NÀY: trỏ về domain]
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao
) {
    // --- PHẦN GIAO DỊCH (TRANSACTION) ---
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    // Lấy thu nhập/chi tiêu theo tháng (để vẽ biểu đồ)
    // (Cần bổ sung Query này bên DAO sau, tạm thời lấy all để filter ở ViewModel)

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getById(id)
    }

    // --- PHẦN SỔ NỢ (DEBT) ---
    // Lấy danh sách người đang nợ mình
    val debtors: Flow<List<DebtEntity>> = debtDao.getDebtors()

    // Lấy danh sách mình đang nợ người ta
    val creditors: Flow<List<DebtEntity>> = debtDao.getCreditors()

    suspend fun insertDebt(debt: DebtEntity) {
        debtDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: DebtEntity) {
        debtDao.updateDebt(debt)
    }
}