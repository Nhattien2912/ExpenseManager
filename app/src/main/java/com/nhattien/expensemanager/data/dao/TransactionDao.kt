package com.nhattien.expensemanager.data.dao

import androidx.room.*
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // Đổi tên thành insertTransaction cho khớp Repo
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("""
        SELECT DISTINCT transactions.* FROM transactions 
        INNER JOIN categories ON transactions.categoryId = categories.id
        WHERE LOWER(transactions.note) LIKE '%' || LOWER(:query) || '%' 
        OR LOWER(categories.name) LIKE '%' || LOWER(:query) || '%'
        ORDER BY transactions.date DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("""
        SELECT DISTINCT transactions.* FROM transactions 
        INNER JOIN categories ON transactions.categoryId = categories.id
        WHERE (LOWER(transactions.note) LIKE '%' || LOWER(:query) || '%' OR LOWER(categories.name) LIKE '%' || LOWER(:query) || '%')
        AND transactions.date BETWEEN :startDate AND :endDate
        ORDER BY transactions.date DESC
    """)
    fun searchTransactionsInRange(query: String, startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionWithCategory?

    // --- FOR WORKER ---
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTransactionsInRangeSync(startDate: Long, endDate: Long): List<TransactionEntity>

    // --- FOR BACKUP/RESTORE ---
    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}