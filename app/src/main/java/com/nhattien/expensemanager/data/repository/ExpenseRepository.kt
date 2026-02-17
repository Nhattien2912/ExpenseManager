package com.nhattien.expensemanager.data.repository

import com.nhattien.expensemanager.data.dao.DebtDao
import com.nhattien.expensemanager.data.dao.TransactionDao
import com.nhattien.expensemanager.data.entity.DebtEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.Flow

import com.nhattien.expensemanager.data.dao.TagDao
import com.nhattien.expensemanager.data.entity.TransactionTagCrossRef

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao,
    private val tagDao: TagDao,
    private val walletDao: com.nhattien.expensemanager.data.dao.WalletDao,
    private val searchHistoryDao: com.nhattien.expensemanager.data.dao.SearchHistoryDao
) {
    // --- PHẦN GIAO DỊCH (TRANSACTION) ---
    val allTransactions: Flow<List<TransactionWithCategory>> = transactionDao.getAllTransactions()
    val allTags = tagDao.getAllTags() // Added

    // --- TÌM KIẾM (SEARCH) ---
    val recentSearches = searchHistoryDao.getRecentSearches()

    fun searchTransactions(query: String): Flow<List<TransactionWithCategory>> {
        return transactionDao.searchTransactions(query)
    }

    fun searchTransactionsInRange(query: String, startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>> {
        return transactionDao.searchTransactionsInRange(query, startDate, endDate)
    }

    suspend fun insertSearchHistory(query: String) {
        searchHistoryDao.insertSearch(com.nhattien.expensemanager.data.entity.SearchHistoryEntity(query = query))
    }

    suspend fun deleteSearchHistory(item: com.nhattien.expensemanager.data.entity.SearchHistoryEntity) {
        searchHistoryDao.deleteSearch(item)
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearHistory()
    }

    // Lấy thu nhập/chi tiêu theo tháng (để vẽ biểu đồ)
    // (Cần bổ sung Query này bên DAO sau, tạm thời lấy all để filter ở ViewModel)

    suspend fun insertTransaction(transaction: TransactionEntity, tagIds: List<Long> = emptyList()) {
        val id = transactionDao.insertTransaction(transaction)
        if (tagIds.isNotEmpty()) {
            tagIds.forEach { tagId ->
                tagDao.insertTransactionTagCrossRef(TransactionTagCrossRef(id, tagId))
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity, tagIds: List<Long> = emptyList()) {
        transactionDao.updateTransaction(transaction)
        // Update tags: Clear old -> Insert new
        tagDao.clearTagsForTransaction(transaction.id)
        if (tagIds.isNotEmpty()) {
            tagIds.forEach { tagId ->
                tagDao.insertTransactionTagCrossRef(TransactionTagCrossRef(transaction.id, tagId))
            }
        }
    }

    suspend fun getTransactionById(id: Long): com.nhattien.expensemanager.data.entity.TransactionWithCategory? {
        return transactionDao.getById(id)
    }
    
    fun getTransactionWithTags(id: Long): Flow<com.nhattien.expensemanager.data.model.TransactionWithTags> {
        return tagDao.getTransactionWithTags(id)
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

    // --- PHẦN VÍ (WALLETS) ---
    val allWallets = walletDao.getAllWallets()

    suspend fun insertWallet(wallet: com.nhattien.expensemanager.data.entity.WalletEntity) {
        walletDao.insertWallet(wallet)
    }

    suspend fun updateWallet(wallet: com.nhattien.expensemanager.data.entity.WalletEntity) {
        walletDao.updateWallet(wallet)
    }

    suspend fun deleteWallet(wallet: com.nhattien.expensemanager.data.entity.WalletEntity) {
        // Soft delete is safer? But here we allow hard delete if no transactions? 
        // Or just archive. Let's start with archive as safer.
        // Actually Dao has archiveWallet.
        walletDao.deleteWallet(wallet)
    }
    
    suspend fun archiveWallet(id: Long) {
        walletDao.archiveWallet(id)
    }
    
    suspend fun getWalletById(id: Long): com.nhattien.expensemanager.data.entity.WalletEntity? {
        return walletDao.getWalletById(id)
    }
}