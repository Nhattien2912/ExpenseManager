package com.nhattien.expensemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.nhattien.expensemanager.domain.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = com.nhattien.expensemanager.data.entity.DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = androidx.room.ForeignKey.SET_NULL
        ),
        androidx.room.ForeignKey(
            entity = com.nhattien.expensemanager.data.entity.WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index("debtId"),
        androidx.room.Index("walletId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val amount: Double = 0.0,

    // Link to CategoryEntity
    val categoryId: Long,
    
    // Store localized copy for easier display (Optional, but helps with Backup/Restore logic if Category is deleted)
    // Actually, just ID is standard. But user wants Custom Categories. ID is safer.
    // If we want "Tiền mặt / Chuyển khoản":
    val paymentMethod: String = "CASH", // "CASH", "BANK"

    val type: TransactionType = TransactionType.EXPENSE,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val debtId: Long? = null,
    
    // New Fields for Multi-Wallet
    val walletId: Long = 1L, // Default to Main Wallet (ID 1)
    val targetWalletId: Long? = null // For TRANSFER only
)