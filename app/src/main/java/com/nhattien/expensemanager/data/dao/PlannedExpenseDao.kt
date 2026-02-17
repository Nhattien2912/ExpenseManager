package com.nhattien.expensemanager.data.dao

import androidx.room.*
import com.nhattien.expensemanager.data.entity.PlannedExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedExpenseDao {

    @Query("SELECT * FROM planned_expenses ORDER BY isCompleted ASC, dueDate ASC")
    fun getAll(): Flow<List<PlannedExpenseEntity>>

    @Query("SELECT * FROM planned_expenses WHERE groupName = :group ORDER BY isCompleted ASC, dueDate ASC")
    fun getByGroup(group: String): Flow<List<PlannedExpenseEntity>>

    @Query("SELECT DISTINCT groupName FROM planned_expenses ORDER BY groupName ASC")
    fun getAllGroups(): Flow<List<String>>

    @Query("SELECT * FROM planned_expenses WHERE id = :id")
    suspend fun getById(id: Long): PlannedExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlannedExpenseEntity): Long

    @Update
    suspend fun update(item: PlannedExpenseEntity)

    @Delete
    suspend fun delete(item: PlannedExpenseEntity)

    @Query("DELETE FROM planned_expenses WHERE groupName = :group")
    suspend fun deleteGroup(group: String)

    @Query("SELECT SUM(amount) FROM planned_expenses WHERE groupName = :group")
    fun getTotalByGroup(group: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM planned_expenses WHERE groupName = :group AND isCompleted = 1")
    fun getCompletedTotalByGroup(group: String): Flow<Double?>
}
