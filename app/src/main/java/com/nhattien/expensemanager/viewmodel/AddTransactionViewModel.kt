package com.nhattien.expensemanager.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import com.nhattien.expensemanager.data.repository.CategoryRepository
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val repository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val transaction = MutableLiveData<TransactionWithCategory?>()

    // Expose Categories
    private val _allCategories = MutableLiveData<List<CategoryEntity>>()
    val allCategories: androidx.lifecycle.LiveData<List<CategoryEntity>> = _allCategories

    // Expose Tags
    private val _allTags = MutableLiveData<List<com.nhattien.expensemanager.data.entity.TagEntity>>()
    val allTags: androidx.lifecycle.LiveData<List<com.nhattien.expensemanager.data.entity.TagEntity>> = _allTags
    
    // Expose Wallets
    private val _allWallets = MutableLiveData<List<com.nhattien.expensemanager.data.entity.WalletEntity>>()
    val allWallets: androidx.lifecycle.LiveData<List<com.nhattien.expensemanager.data.entity.WalletEntity>> = _allWallets

    init {
        loadCategories()
        loadTags()
        loadWallets()
    }
    
    private fun loadWallets() {
        viewModelScope.launch {
            repository.allWallets.collect {
                _allWallets.postValue(it)
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            repository.allTags.collect {
                _allTags.postValue(it)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _allCategories.postValue(categoryRepository.getAllCategories())
        }
    }

    fun addTransaction(
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        paymentMethod: String,
        note: String?,
        date: Long,             
        isRecurring: Boolean,   
        tagIds: List<Long> = emptyList(),
        walletId: Long, // Added
        targetWalletId: Long? = null, // Added
        onSuccess: () -> Unit
    ) {
        val entity = TransactionEntity(
            amount = amount,
            type = type,
            categoryId = categoryId,
            paymentMethod = paymentMethod,
            note = note ?: "",
            date = date,           
            isRecurring = isRecurring,
            walletId = walletId, // Added
            targetWalletId = targetWalletId // Added
        )

        viewModelScope.launch {
            repository.insertTransaction(entity, tagIds)
            onSuccess()
        }
    }

    fun updateTransaction(
        id: Long,
        amount: Double,
        type: TransactionType,
        categoryId: Long,
        paymentMethod: String,
        note: String?,
        date: Long,
        isRecurring: Boolean,
        tagIds: List<Long> = emptyList(),
        walletId: Long, // Added
        targetWalletId: Long? = null, // Added
        onSuccess: () -> Unit
    ) {
        val entity = TransactionEntity(
            id = id,
            amount = amount,
            type = type,
            categoryId = categoryId,
            paymentMethod = paymentMethod,
            note = note ?: "",
            date = date,
            isRecurring = isRecurring,
            walletId = walletId, // Added
            targetWalletId = targetWalletId // Added
        )
        viewModelScope.launch {
            repository.updateTransaction(entity, tagIds)
            onSuccess()
        }
    }

    // Expose Transaction Tags for Edit
    private val _transactionTags = MutableLiveData<List<com.nhattien.expensemanager.data.entity.TagEntity>>()
    val transactionTags: androidx.lifecycle.LiveData<List<com.nhattien.expensemanager.data.entity.TagEntity>> = _transactionTags

    fun getTransaction(id: Long) {
        viewModelScope.launch {
            val result = repository.getTransactionById(id)
            transaction.postValue(result)
        }
        
        // Fetch Tags
        viewModelScope.launch {
            repository.getTransactionWithTags(id).collect {
                _transactionTags.postValue(it.tags)
            }
        }
    }

    fun deleteTransaction(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Needed to get entity ref or just delete by ID if DAO supports it?
            // Repository expecting Entity. Let's fetch then delete or assume we have it.
            // Since we need to delete, we should probably fetch it first or use a method that deletes by ID.
            // But Repository.deleteTransaction takes an Entity.
            // Let's get it first.
             val entity = repository.getTransactionById(id)
             if (entity != null) {
                 repository.deleteTransaction(entity.transaction)
                 onSuccess()
             }
        }
    }

    fun addCategory(name: String, icon: String, type: TransactionType, onSuccess: () -> Unit) {
        viewModelScope.launch {
            categoryRepository.insertCategory(CategoryEntity(name = name, icon = icon, type = type))
            loadCategories() // Refresh list immediately
            onSuccess()
        }
    }
}