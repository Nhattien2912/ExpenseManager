package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.RecurringTransactionEntity
import com.nhattien.expensemanager.data.repository.RecurringTransactionRepository
import kotlinx.coroutines.launch

class ManageRecurringViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecurringTransactionRepository

    private val _recurringList = MutableLiveData<List<RecurringTransactionEntity>>()
    val recurringList: LiveData<List<RecurringTransactionEntity>> = _recurringList

    init {
        val recurringDao = AppDatabase.getInstance(application).recurringTransactionDao()
        repository = RecurringTransactionRepository(recurringDao)
        loadRecurringTransactions()
    }

    private fun loadRecurringTransactions() {
        viewModelScope.launch {
            repository.allRecurring.collect { list ->
                _recurringList.postValue(list)
            }
        }
    }

    fun deleteRecurring(transaction: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun updateStatus(id: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateStatus(id, isActive)
        }
    }
}
