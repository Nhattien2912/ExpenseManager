package com.nhattien.expensemanager.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.PlannedExpenseEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.entity.WalletEntity
import com.nhattien.expensemanager.data.repository.CategoryRepository
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlannedExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val plannedDao = db.plannedExpenseDao()
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val walletDao = db.walletDao()
    private val categoryRepository = CategoryRepository(categoryDao)

    private val prefs = application.getSharedPreferences("planned_groups", Context.MODE_PRIVATE)

    private val _currentGroup = MutableStateFlow<String?>(null)
    val currentGroup: StateFlow<String?> = _currentGroup

    private val _savedGroups = MutableStateFlow(loadSavedGroups())

    val allGroups: StateFlow<List<String>> = combine(
        plannedDao.getAllGroups(),
        _savedGroups
    ) { dbGroups, saved ->
        (dbGroups + saved).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentItems: StateFlow<List<PlannedExpenseEntity>> = _currentGroup
        .flatMapLatest { group ->
            if (group != null) plannedDao.getByGroup(group) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val groupTotal: StateFlow<Double> = _currentGroup
        .flatMapLatest { group ->
            if (group != null) plannedDao.getTotalByGroup(group).map { it ?: 0.0 } else flowOf(0.0)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val completedTotal: StateFlow<Double> = _currentGroup
        .flatMapLatest { group ->
            if (group != null) plannedDao.getCompletedTotalByGroup(group).map { it ?: 0.0 } else flowOf(0.0)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val expenseCategories: StateFlow<List<CategoryEntity>> = categoryDao
        .observeCategoriesByType(TransactionType.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val wallets: StateFlow<List<WalletEntity>> = walletDao
        .getAllWallets()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categoryMap: StateFlow<Map<Long, CategoryEntity>> = categoryDao
        .observeAllCategories()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val walletMap: StateFlow<Map<Long, WalletEntity>> = walletDao
        .getAllWallets()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    init {
        // Ensure default categories exist so Planned dialog always has data on first install.
        viewModelScope.launch {
            categoryRepository.getAllCategories()
        }
    }

    private fun loadSavedGroups(): Set<String> {
        return prefs.getStringSet("groups", emptySet()) ?: emptySet()
    }

    private fun persistGroups(groups: Set<String>) {
        prefs.edit().putStringSet("groups", groups).apply()
        _savedGroups.value = groups
    }

    fun selectGroup(group: String) {
        _currentGroup.value = group
    }

    fun addItem(title: String, amount: Double, categoryId: Long, walletId: Long, note: String, dueDate: Long) {
        val group = _currentGroup.value ?: return
        viewModelScope.launch {
            plannedDao.insert(
                PlannedExpenseEntity(
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    walletId = walletId,
                    note = note,
                    dueDate = dueDate,
                    groupName = group
                )
            )
        }
    }

    fun deleteItem(item: PlannedExpenseEntity) {
        viewModelScope.launch {
            if (item.isCompleted && item.transactionId != null) {
                val txn = transactionDao.getById(item.transactionId)
                if (txn != null) {
                    transactionDao.deleteTransaction(txn.transaction)
                }
            }
            plannedDao.delete(item)
        }
    }

    fun toggleComplete(item: PlannedExpenseEntity) {
        viewModelScope.launch {
            if (!item.isCompleted) {
                val txnId = transactionDao.insertTransaction(
                    TransactionEntity(
                        amount = item.amount,
                        categoryId = item.categoryId,
                        type = TransactionType.EXPENSE,
                        note = buildPlannedNote(item.title, item.note),
                        date = System.currentTimeMillis(),
                        walletId = item.walletId
                    )
                )
                plannedDao.update(item.copy(isCompleted = true, transactionId = txnId))
            } else {
                if (item.transactionId != null) {
                    val txn = transactionDao.getById(item.transactionId)
                    if (txn != null) {
                        transactionDao.deleteTransaction(txn.transaction)
                    }
                }
                plannedDao.update(item.copy(isCompleted = false, transactionId = null))
            }
        }
    }

    fun updateItem(
        item: PlannedExpenseEntity,
        title: String,
        amount: Double,
        categoryId: Long,
        walletId: Long,
        note: String,
        dueDate: Long
    ) {
        viewModelScope.launch {
            val updatedItem = item.copy(
                title = title,
                amount = amount,
                categoryId = categoryId,
                walletId = walletId,
                note = note,
                dueDate = dueDate
            )
            plannedDao.update(updatedItem)

            val txnId = updatedItem.transactionId
            if (updatedItem.isCompleted && txnId != null) {
                val txn = transactionDao.getById(txnId)?.transaction
                if (txn != null) {
                    transactionDao.updateTransaction(
                        txn.copy(
                            amount = updatedItem.amount,
                            categoryId = updatedItem.categoryId,
                            note = buildPlannedNote(updatedItem.title, updatedItem.note),
                            walletId = updatedItem.walletId
                        )
                    )
                }
            }
        }
    }

    fun createGroup(rawName: String) {
        val name = rawName.trim().replace(Regex("\\s+"), " ")
        if (name.isEmpty()) return

        val existing = (_savedGroups.value + allGroups.value).firstOrNull {
            it.equals(name, ignoreCase = true)
        }

        if (existing != null) {
            _currentGroup.value = existing
            return
        }

        val updated = _savedGroups.value + name
        persistGroups(updated)
        _currentGroup.value = name
    }

    fun deleteGroup(group: String) {
        viewModelScope.launch {
            plannedDao.getByGroup(group).first().forEach { item ->
                if (item.isCompleted && item.transactionId != null) {
                    val txn = transactionDao.getById(item.transactionId)
                    if (txn != null) {
                        transactionDao.deleteTransaction(txn.transaction)
                    }
                }
            }
            plannedDao.deleteGroup(group)
            val updated = _savedGroups.value - group
            persistGroups(updated)
            _currentGroup.value = null
        }
    }

    private fun buildPlannedNote(title: String, note: String): String {
        return "Dự tính: $title" + if (note.isNotEmpty()) " - $note" else ""
    }
}
