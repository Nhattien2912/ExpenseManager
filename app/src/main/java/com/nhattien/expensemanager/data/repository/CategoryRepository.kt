package com.nhattien.expensemanager.data.repository

import com.nhattien.expensemanager.data.dao.CategoryDao
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.domain.Category
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CategoryRepository(private val categoryDao: CategoryDao) {

    suspend fun getAllCategories(): List<CategoryEntity> {
        return withContext(Dispatchers.IO) {
            var categories = categoryDao.getAllCategories()
            if (categories.isEmpty()) {
                initDefaultCategories()
                categories = categoryDao.getAllCategories()
            }
            categories
        }
    }

    suspend fun getCategoriesByType(type: TransactionType): List<CategoryEntity> {
        return withContext(Dispatchers.IO) {
            var categories = categoryDao.getCategoriesByType(type)
            if (categories.isEmpty()) {
                initDefaultCategories()
                categories = categoryDao.getCategoriesByType(type)
            }
            categories
        }
    }

    suspend fun getCategoryByName(name: String): CategoryEntity? {
        return withContext(Dispatchers.IO) {
            categoryDao.getCategoryByName(name)
        }
    }

    suspend fun addCategory(name: String, icon: String, type: TransactionType) {
        withContext(Dispatchers.IO) {
            val entity = CategoryEntity(name = name, icon = icon, type = type, isDefault = false)
            categoryDao.insert(entity)
        }
    }
    
    suspend fun restoreCategories(categories: List<CategoryEntity>) {
        withContext(Dispatchers.IO) {
            categoryDao.deleteAll()
            categoryDao.insertAll(categories)
        }
    }

    suspend fun insertCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            categoryDao.insert(category)
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        withContext(Dispatchers.IO) {
            categoryDao.delete(category)
        }
    }

    private fun initDefaultCategories() {
        // Convert old Enum to new Entity list
        val defaultList = Category.values().map { 
            val type = when (it.group) {
                com.nhattien.expensemanager.domain.TypeGroup.INCOME -> TransactionType.INCOME
                com.nhattien.expensemanager.domain.TypeGroup.DEBT -> {
                     // Map specific debt categories
                     if (it == Category.LENDING || it == Category.DEBT_REPAYMENT) TransactionType.LOAN_GIVE
                     else TransactionType.LOAN_TAKE
                }
                else -> TransactionType.EXPENSE
            }

            CategoryEntity(
                name = it.label,
                icon = it.icon,
                type = type,
                isDefault = true
            )
        }
        categoryDao.insertAll(defaultList)
    }
}
