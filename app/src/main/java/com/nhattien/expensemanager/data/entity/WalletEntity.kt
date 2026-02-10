package com.nhattien.expensemanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val initialBalance: Double = 0.0,
    val icon: String = "💰", // Default icon
    val color: Int = android.graphics.Color.BLUE, // Default color
    val isArchived: Boolean = false
)
