package com.nhattien.expensemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nhattien.expensemanager.data.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    // Lấy danh sách tất cả khoản nợ
    @Query("SELECT * FROM debts ORDER BY startDate DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    // Lấy danh sách người đang NỢ MÌNH (Mình cho vay)
    @Query("SELECT * FROM debts WHERE isMeLending = 1 AND isFinished = 0")
    fun getDebtors(): Flow<List<DebtEntity>>

    // Lấy danh sách MÌNH ĐANG NỢ (Đi vay)
    @Query("SELECT * FROM debts WHERE isMeLending = 0 AND isFinished = 0")
    fun getCreditors(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}