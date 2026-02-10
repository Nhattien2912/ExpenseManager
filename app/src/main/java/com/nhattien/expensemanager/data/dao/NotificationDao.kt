package com.nhattien.expensemanager.data.dao

import androidx.room.*
import com.nhattien.expensemanager.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long
    
    @Update
    suspend fun update(notification: NotificationEntity)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)
    
    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
    
    @Delete
    suspend fun delete(notification: NotificationEntity)
    
    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
    
    @Query("DELETE FROM notifications WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}
