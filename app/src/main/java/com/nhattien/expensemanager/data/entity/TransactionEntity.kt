package com.nhattien.expensemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nhattien.expensemanager.domain.Category // Import Enum Category
import com.nhattien.expensemanager.domain.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val amount: Double = 0.0,

    // ===> SỬA DÒNG NÀY: Dùng Enum Category thay vì String
    val category: Category = Category.OTHER_EXPENSE,

    val type: TransactionType = TransactionType.EXPENSE,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val debtId: Long? = null
)