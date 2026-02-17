package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.Normalizer
import java.util.Locale

data class SavingsBucketItem(
    val name: String,
    val deposited: Double,
    val withdrawn: Double,
    val transactionCount: Int
) {
    val balance: Double
        get() = deposited - withdrawn
}

class SavingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao(), db.tagDao(), db.walletDao(), db.searchHistoryDao())
    }

    val savingTransactions: StateFlow<List<TransactionWithCategory>> = repository.allTransactions
        .map { list ->
            list.filter { it.isSavingTransaction() }
                .sortedByDescending { it.transaction.date }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalSavings: StateFlow<Double> = savingTransactions
        .map { list ->
            val deposited = list.filter { it.category.isSavingInCategory() }
                .sumOf { it.transaction.amount }
            val withdrawn = list.filter { it.category.isSavingOutCategory() }
                .sumOf { it.transaction.amount }
            deposited - withdrawn
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val savingBuckets: StateFlow<List<SavingsBucketItem>> = savingTransactions
        .map { list ->
            list.groupBy { resolveBucketName(it) }
                .map { (bucketName, transactions) ->
                    val deposited = transactions
                        .filter { it.category.isSavingInCategory() }
                        .sumOf { it.transaction.amount }
                    val withdrawn = transactions
                        .filter { it.category.isSavingOutCategory() }
                        .sumOf { it.transaction.amount }

                    SavingsBucketItem(
                        name = bucketName,
                        deposited = deposited,
                        withdrawn = withdrawn,
                        transactionCount = transactions.size
                    )
                }
                .sortedByDescending { it.balance }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun resolveBucketName(item: TransactionWithCategory): String {
        val tagBucket = item.tags
            .map { it.name.trim() }
            .firstOrNull { it.isNotEmpty() }
        if (tagBucket != null) return tagBucket

        val note = item.transaction.note.trim()
        val bracketBucket = Regex("^\\[(.+?)]").find(note)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        if (!bracketBucket.isNullOrEmpty()) return bracketBucket

        return "Chung"
    }

    private fun TransactionWithCategory.isSavingTransaction(): Boolean {
        return category.isSavingInCategory() || category.isSavingOutCategory()
    }

    private fun CategoryEntity.isSavingInCategory(): Boolean {
        val normalized = normalizeText(name)
        return normalized.contains("gui tiet kiem")
    }

    private fun CategoryEntity.isSavingOutCategory(): Boolean {
        val normalized = normalizeText(name)
        return normalized.contains("rut tiet kiem")
    }

    private fun normalizeText(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return decomposed
            .replace("\\p{M}+".toRegex(), "")
            .replace('đ', 'd')
            .replace('Đ', 'd')
            .lowercase(Locale.ROOT)
    }
}
