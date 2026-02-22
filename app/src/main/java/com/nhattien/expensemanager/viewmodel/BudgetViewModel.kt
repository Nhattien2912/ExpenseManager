package com.nhattien.expensemanager.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.CategoryRepository
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategoryBudgetLimitItem(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val spent: Double,
    val limit: Double
) {
    val remaining: Double
        get() = limit - spent

    val isExceeded: Boolean
        get() = limit > 0 && spent > limit

    val progressPercent: Int
        get() = if (limit <= 0) 0 else ((spent / limit) * 100).toInt()
}

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(db.transactionDao(), db.debtDao(), db.tagDao(), db.walletDao(), db.searchHistoryDao(), db.categoryDao())
    private val categoryRepository = CategoryRepository(db.categoryDao())
    private val categoryDao = db.categoryDao()
    private val allTransactions = repository.allTransactions
    private val prefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _spendingLimit = MutableStateFlow(prefs.getFloat(KEY_SPENDING_LIMIT, 5000000f).toDouble())
    val spendingLimit: StateFlow<Double> = _spendingLimit

    private val _categoryLimits = MutableStateFlow(readCategoryLimitMap())
    val categoryLimits: StateFlow<Map<Long, Double>> = _categoryLimits

    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_SPENDING_LIMIT -> {
                _spendingLimit.value = prefs.getFloat(KEY_SPENDING_LIMIT, 5000000f).toDouble()
            }

            KEY_CATEGORY_LIMITS -> {
                _categoryLimits.value = readCategoryLimitMap()
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    val totalReceivable = allTransactions.map { list ->
        val given = list.filter {
            it.transaction.type == TransactionType.LOAN_GIVE && isCategoryName(it.category.name, "cho vay")
        }.sumOf { it.transaction.amount }

        val collected = list.filter {
            it.transaction.type == TransactionType.LOAN_TAKE && isCategoryName(it.category.name, "thu no")
        }.sumOf { it.transaction.amount }

        given - collected
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val totalPayable = allTransactions.map { list ->
        val borrowed = list.filter {
            it.transaction.type == TransactionType.LOAN_TAKE && isCategoryName(it.category.name, "di vay")
        }.sumOf { it.transaction.amount }

        val repaid = list.filter {
            it.transaction.type == TransactionType.LOAN_GIVE && isCategoryName(it.category.name, "tra no")
        }.sumOf { it.transaction.amount }

        borrowed - repaid
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val debtTransactions = allTransactions.map { list ->
        list.filter {
            it.transaction.type == TransactionType.LOAN_GIVE || it.transaction.type == TransactionType.LOAN_TAKE
        }.sortedByDescending { it.transaction.date }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun settleDebt(origin: TransactionEntity) {
        viewModelScope.launch {
            val targetKeyword = if (origin.type == TransactionType.LOAN_GIVE) "thu no" else "tra no"
            val targetCategory = findCategoryByKeyword(targetKeyword) ?: return@launch

            val (newType, notePrefix) = if (origin.type == TransactionType.LOAN_GIVE) {
                Pair(TransactionType.LOAN_TAKE, "Da thu no khoan")
            } else {
                Pair(TransactionType.LOAN_GIVE, "Da tra no khoan")
            }

            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(origin.date))

            val settleTransaction = TransactionEntity(
                amount = origin.amount,
                type = newType,
                categoryId = targetCategory.id,
                note = "$notePrefix $dateStr",
                date = System.currentTimeMillis()
            )
            repository.insertTransaction(settleTransaction)
        }
    }

    val expenseCategories: StateFlow<List<CategoryEntity>> = categoryDao
        .observeCategoriesByType(TransactionType.EXPENSE)
        .map { list -> list.filterNot { it.isSavingCategory() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val currentMonthExpenseByCategory: StateFlow<Map<Long, Double>> = allTransactions
        .map { list ->
            list.asSequence()
                .filter { item ->
                    item.transaction.type == TransactionType.EXPENSE &&
                        item.category.isSavingCategory().not() &&
                        isInCurrentMonth(item.transaction.date)
                }
                .groupBy { it.category.id }
                .mapValues { entry -> entry.value.sumOf { it.transaction.amount } }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val currentMonthExpense = allTransactions.map { list ->
        list.filter { item ->
            item.transaction.type == TransactionType.EXPENSE &&
                item.category.isSavingCategory().not() &&
                isInCurrentMonth(item.transaction.date)
        }.sumOf { it.transaction.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val categoryLimitItems: StateFlow<List<CategoryBudgetLimitItem>> = combine(
        expenseCategories,
        currentMonthExpenseByCategory,
        _categoryLimits
    ) { categories, spentByCategory, limits ->
        categories.mapNotNull { category ->
            val limit = limits[category.id] ?: return@mapNotNull null
            CategoryBudgetLimitItem(
                categoryId = category.id,
                categoryName = category.name,
                categoryIcon = category.icon,
                spent = spentByCategory[category.id] ?: 0.0,
                limit = limit
            )
        }.sortedByDescending { item ->
            if (item.limit > 0) item.spent / item.limit else 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSpendingLimit(amount: Double) {
        prefs.edit().putFloat(KEY_SPENDING_LIMIT, amount.toFloat()).apply()
        _spendingLimit.value = amount
    }

    fun setCategorySpendingLimit(categoryId: Long, amount: Double) {
        if (amount <= 0.0) {
            clearCategorySpendingLimit(categoryId)
            return
        }
        val updated = _categoryLimits.value.toMutableMap()
        updated[categoryId] = amount
        persistCategoryLimitMap(updated)
    }

    fun clearCategorySpendingLimit(categoryId: Long) {
        val updated = _categoryLimits.value.toMutableMap()
        updated.remove(categoryId)
        persistCategoryLimitMap(updated)
    }

    suspend fun getExpenseCategoriesNow(): List<CategoryEntity> {
        return categoryRepository.getAllCategories()
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .filterNot { it.isSavingCategory() }
            .toList()
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private suspend fun findCategoryByKeyword(keyword: String): CategoryEntity? {
        val categories = categoryRepository.getAllCategories()
        return categories.firstOrNull { isCategoryName(it.name, keyword) }
    }

    private fun isCategoryName(name: String, keyword: String): Boolean {
        return normalizeText(name).contains(keyword)
    }

    private fun isInCurrentMonth(timestamp: Long): Boolean {
        val now = Calendar.getInstance()
        val txDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return now.get(Calendar.MONTH) == txDate.get(Calendar.MONTH) &&
            now.get(Calendar.YEAR) == txDate.get(Calendar.YEAR)
    }

    private fun readCategoryLimitMap(): Map<Long, Double> {
        val raw = prefs.getString(KEY_CATEGORY_LIMITS, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            val result = mutableMapOf<Long, Double>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val id = key.toLongOrNull() ?: continue
                val amount = json.optDouble(key, 0.0)
                if (amount > 0.0) {
                    result[id] = amount
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun persistCategoryLimitMap(data: Map<Long, Double>) {
        val clean = data.filterValues { it > 0.0 }
        val json = JSONObject()
        clean.forEach { (id, amount) ->
            json.put(id.toString(), amount)
        }
        prefs.edit().putString(KEY_CATEGORY_LIMITS, json.toString()).apply()
        _categoryLimits.value = clean
    }

    private fun CategoryEntity.isSavingCategory(): Boolean {
        val normalized = normalizeText(name)
        return normalized.contains("tiet kiem")
    }

    private fun normalizeText(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return decomposed
            .replace("\\p{M}+".toRegex(), "")
            .replace('\u0111', 'd')
            .replace('\u0110', 'd')
            .lowercase(Locale.ROOT)
    }

    private companion object {
        const val PREF_NAME = "expense_manager"
        const val KEY_SPENDING_LIMIT = "KEY_SPENDING_LIMIT"
        const val KEY_CATEGORY_LIMITS = "KEY_CATEGORY_LIMITS"
    }
}
