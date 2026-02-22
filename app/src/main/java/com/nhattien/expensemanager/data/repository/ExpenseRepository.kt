package com.nhattien.expensemanager.data.repository

import com.nhattien.expensemanager.data.dao.DebtDao
import com.nhattien.expensemanager.data.dao.TransactionDao
import com.nhattien.expensemanager.data.dao.CategoryDao
import com.nhattien.expensemanager.data.entity.DebtEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

import com.nhattien.expensemanager.data.dao.TagDao
import com.nhattien.expensemanager.data.entity.TransactionTagCrossRef

import com.nhattien.expensemanager.data.source.FirestoreDataSource

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao,
    private val tagDao: TagDao,
    private val walletDao: com.nhattien.expensemanager.data.dao.WalletDao,
    private val searchHistoryDao: com.nhattien.expensemanager.data.dao.SearchHistoryDao,
    private val categoryDao: CategoryDao? = null, // Added for category lookup
    private val firestore: FirestoreDataSource = FirestoreDataSource()
) {
    // --- PHẦN GIAO DỊCH (TRANSACTION) ---
    val allTransactions: Flow<List<TransactionWithCategory>> = transactionDao.getAllTransactions()
    val allTags = tagDao.getAllTags()

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

    // Helper: lookup category info
    private suspend fun getCategoryInfo(categoryId: Long): Pair<String, String> {
        val cat = categoryDao?.getCategoryById(categoryId)
        return Pair(cat?.name ?: "Khác", cat?.icon ?: "💰")
    }

    suspend fun insertTransaction(transaction: TransactionEntity, tagIds: List<Long> = emptyList()) {
        val id = transactionDao.insertTransaction(transaction)
        val insertedTransaction = transaction.copy(id = id)

        // Lookup category info and sync to Cloud
        val (catName, catIcon) = getCategoryInfo(transaction.categoryId)
        firestore.saveTransaction(insertedTransaction, catName, catIcon)

        if (tagIds.isNotEmpty()) {
            tagIds.forEach { tagId ->
                tagDao.insertTransactionTagCrossRef(TransactionTagCrossRef(id, tagId))
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        firestore.deleteTransaction(transaction.id)
    }

    suspend fun updateTransaction(transaction: TransactionEntity, tagIds: List<Long> = emptyList()) {
        transactionDao.updateTransaction(transaction)

        // Lookup category info and sync to Cloud
        val (catName, catIcon) = getCategoryInfo(transaction.categoryId)
        firestore.saveTransaction(transaction, catName, catIcon)

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

    // --- SYNC ALL DATA TO FIRESTORE ---
    suspend fun syncAllDataToCloud() {
        val allTx = transactionDao.getAllTransactionsSync()
        val allCats = categoryDao?.getAllCategories() ?: emptyList()
        val allWalletsList = walletDao.getAllWalletsSync()

        // Build a category lookup map
        val catMap = allCats.associateBy { it.id }

        // Create enriched transactions
        val enriched = allTx.map { tx ->
            val cat = catMap[tx.categoryId]
            FirestoreDataSource.EnrichedTransaction(
                transaction = tx,
                categoryName = cat?.name ?: "Khác",
                categoryIcon = cat?.icon ?: "💰"
            )
        }

        firestore.syncAll(enriched, allCats, allWalletsList)
    }

    // --- PHẦN SỔ NỢ (DEBT) ---
    val debtors: Flow<List<DebtEntity>> = debtDao.getDebtors()
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
        val id = walletDao.insertWallet(wallet)
        firestore.saveWallet(wallet.copy(id = id))
    }

    suspend fun updateWallet(wallet: com.nhattien.expensemanager.data.entity.WalletEntity) {
        walletDao.updateWallet(wallet)
        firestore.saveWallet(wallet)
    }

    suspend fun deleteWallet(wallet: com.nhattien.expensemanager.data.entity.WalletEntity) {
        walletDao.deleteWallet(wallet)
        firestore.deleteWallet(wallet.id)
    }

    suspend fun archiveWallet(id: Long) {
        walletDao.archiveWallet(id)
    }

    suspend fun getWalletById(id: Long): com.nhattien.expensemanager.data.entity.WalletEntity? {
        return walletDao.getWalletById(id)
    }

    // --- BIDIRECTIONAL SYNC: Listen for Cloud changes → Room ---
    fun startListeningForCloudChanges(
        scope: kotlinx.coroutines.CoroutineScope
    ): com.google.firebase.firestore.ListenerRegistration? {
        return firestore.listenForTransactionChanges(
            onUpsert = { transaction, _, _ ->
                scope.launch {
                    // Insert or update in Room (REPLACE strategy)
                    transactionDao.insertTransaction(transaction)
                }
            },
            onDelete = { docId ->
                scope.launch {
                    val roomId = docId.toLongOrNull() ?: docId.hashCode().toLong()
                    val existing = transactionDao.getById(roomId)
                    if (existing != null) {
                        transactionDao.deleteTransaction(existing.transaction)
                    }
                }
            }
        )
    }
}