package com.nhattien.expensemanager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nhattien.expensemanager.data.entity.TagEntity
import com.nhattien.expensemanager.data.entity.TransactionTagCrossRef
import com.nhattien.expensemanager.data.model.TransactionWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) AND id != :excludeId LIMIT 1")
    suspend fun getTagByNameExceptId(name: String, excludeId: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionTagCrossRef(crossRef: TransactionTagCrossRef)

    @Delete
    suspend fun deleteTransactionTagCrossRef(crossRef: TransactionTagCrossRef)
    
    @Query("DELETE FROM transaction_tag_cross_ref WHERE transactionId = :transactionId")
    suspend fun clearTagsForTransaction(transactionId: Long)

    @Transaction // Required for @Relation
    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    fun getTransactionWithTags(transactionId: Long): Flow<TransactionWithTags>
}
