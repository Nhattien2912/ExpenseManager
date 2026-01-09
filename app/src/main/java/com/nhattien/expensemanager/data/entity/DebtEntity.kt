package com.nhattien.expensemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val debtorName: String,       // Tên người
    val amount: Double,           // Số tiền gốc
    val amountPaid: Double = 0.0, // Số tiền đã trả
    val isMeLending: Boolean,     // True = Mình cho vay, False = Mình đi vay
    val startDate: Long,
    val dueDate: Long?,
    val interestRate: Double = 0.0,
    val isFinished: Boolean = false
)