package com.nhattien.expensemanager.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.nhattien.expensemanager.data.entity.TagEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.entity.TransactionTagCrossRef

data class TransactionWithTags(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TransactionTagCrossRef::class,
            parentColumn = "transactionId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
