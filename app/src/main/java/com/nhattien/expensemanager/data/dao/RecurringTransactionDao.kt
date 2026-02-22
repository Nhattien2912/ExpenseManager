package com.nhattien.expensemanager.data.dao

import androidx.room.*
import com.nhattien.expensemanager.data.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY nextRunDate ASC")
    fun getAllRecurring(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextRunDate <= :currentTime")
    suspend fun getDueTransactions(currentTime: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextRunDate > :now AND nextRunDate <= :deadline")
    suspend fun getUpcomingTransactions(now: Long, deadline: Long): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: RecurringTransactionEntity): Long

    @Update
    suspend fun update(transaction: RecurringTransactionEntity)

    @Delete
    suspend fun delete(transaction: RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET isActive = :isActive WHERE id = :id")
    suspend fun updateStatus(id: Long, isActive: Boolean)
}
