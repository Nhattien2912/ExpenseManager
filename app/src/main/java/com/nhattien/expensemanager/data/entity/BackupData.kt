package com.nhattien.expensemanager.data.entity

data class BackupData(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
)
