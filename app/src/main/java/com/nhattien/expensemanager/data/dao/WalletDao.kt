package com.nhattien.expensemanager.data.dao

import androidx.room.*
import com.nhattien.expensemanager.data.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE isArchived = 0")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity)
    
    // Soft Delete
    @Query("UPDATE wallets SET isArchived = 1 WHERE id = :id")
    suspend fun archiveWallet(id: Long)

    @Query("SELECT * FROM wallets WHERE isArchived = 0")
    suspend fun getAllWalletsSync(): List<WalletEntity>
}
