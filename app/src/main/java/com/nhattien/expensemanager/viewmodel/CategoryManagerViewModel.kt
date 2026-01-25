package com.nhattien.expensemanager.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.repository.CategoryRepository
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.launch

class CategoryManagerViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _categories = MutableLiveData<List<CategoryEntity>>()
    val categories: LiveData<List<CategoryEntity>> = _categories

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _categories.postValue(repository.getAllCategories())
        }
    }

    fun addCategory(name: String, icon: String, type: TransactionType, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.addCategory(name, icon, type)
            onSuccess()
        }
    }

    fun deleteCategory(category: CategoryEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            onSuccess()
        }
    }

    fun reloadCategories(onLoaded: (List<CategoryEntity>) -> Unit) {
        viewModelScope.launch {
            val list = repository.getAllCategories()
            _categories.postValue(list)
            onLoaded(list)
        }
    }
}
