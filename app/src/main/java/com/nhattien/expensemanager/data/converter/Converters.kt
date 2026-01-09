package com.nhattien.expensemanager.data.converter

import androidx.room.TypeConverter
import com.nhattien.expensemanager.domain.Category
import com.nhattien.expensemanager.domain.TransactionType

class Converters {
    // ===== TransactionType =====
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    // ===== Category =====
    @TypeConverter
    fun fromCategory(category: Category): String {
        return category.name
    }

    @TypeConverter
    fun toCategory(value: String): Category {
        return Category.valueOf(value)
    }
}