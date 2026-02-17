package com.nhattien.expensemanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "planned_expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("categoryId"),
        Index("walletId"),
        Index("groupName")
    ]
)
data class PlannedExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val amount: Double,
    val categoryId: Long,
    val walletId: Long = 1L,
    val note: String = "",
    val dueDate: Long = System.currentTimeMillis(),

    val isCompleted: Boolean = false,
    val transactionId: Long? = null, // ID of actual Transaction after completing

    val groupName: String = "Mặc định",
    val createdAt: Long = System.currentTimeMillis()
)

