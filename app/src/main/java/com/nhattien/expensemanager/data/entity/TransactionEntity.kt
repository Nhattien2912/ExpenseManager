package com.nhattien.expensemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val amount: Double,          // số tiền
    val type: String,            // INCOME / EXPENSE
    val category: String,        // Ăn uống, Nhà ở...
    val note: String?,           // ghi chú
    val date: Long               // thời gian
)
