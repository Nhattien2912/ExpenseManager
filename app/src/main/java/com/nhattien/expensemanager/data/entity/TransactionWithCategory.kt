package com.nhattien.expensemanager.data.entity

import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Junction

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TransactionTagCrossRef::class,
            parentColumn = "transactionId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,

    @Relation(
        parentColumn = "walletId",
        entityColumn = "id"
    )
    val wallet: WalletEntity,

    @Relation(
        parentColumn = "targetWalletId",
        entityColumn = "id"
    )
    val targetWallet: WalletEntity?
)
