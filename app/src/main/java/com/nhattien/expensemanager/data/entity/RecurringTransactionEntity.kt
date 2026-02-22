package com.nhattien.expensemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index("walletId")
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double = 0.0,
    val categoryId: Long,
    val type: com.nhattien.expensemanager.domain.TransactionType,
    val note: String = "",
    val walletId: Long = 1L,
    
    // DAILY, WEEKLY, MONTHLY, YEARLY
    val recurrencePeriod: String, 
    
    // When is the next time this should run (in millis)
    val nextRunDate: Long,
    
    val isActive: Boolean = true,
    
    // "PERSONAL" (default) or "BANK" - to separate bank loans from personal debts
    val loanSource: String = "PERSONAL",
    
    // Total number of installments (0 = unlimited, e.g. 24 for a 2-year bank loan)
    val totalInstallments: Int = 0,
    
    // Number of installments already completed (Worker increments this)
    val completedInstallments: Int = 0
)
