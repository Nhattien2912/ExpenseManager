package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.SearchHistoryEntity
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(
            transactionDao = db.transactionDao(),
            debtDao = db.debtDao(),
            tagDao = db.tagDao(),
            walletDao = db.walletDao(),
            searchHistoryDao = db.searchHistoryDao(),
            categoryDao = db.categoryDao()
        )
    }

    // --- INPUTS ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType = _filterType.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val dateRange = _dateRange.asStateFlow()

    private val _amountRange = MutableStateFlow<Pair<Double, Double>?>(null)
    val amountRange = _amountRange.asStateFlow()

    // --- OUTPUTS ---
    val recentSearches: StateFlow<List<SearchHistoryEntity>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<TransactionWithCategory>> = combine(
        _searchQuery.debounce(300), // Wait 300ms after typing stops
        _dateRange,
        _filterType,
        _amountRange
    ) { query, dates, type, amount ->
        SearchResultParams(query, dates, type, amount)
    }.flatMapLatest { params ->
        if (params.query.isBlank() && params.dates == null) {
            flowOf(emptyList()) // No query, no results
        } else {
            // 1. Base Search (Query + Date)
            val baseFlow = if (params.dates != null) {
                repository.searchTransactionsInRange(params.query, params.dates.first, params.dates.second)
            } else {
                repository.searchTransactions(params.query)
            }

            // 2. Memory Filter (Type + Amount)
            // Room is handling Text + Date filtering. We handle Type + Amount in memory for simplicity.
            // Complex SQL for Type + Amount is possible but risky with changing schemas.
            baseFlow // This returns Flow<List<T>>
                .combine(flowOf(params)) { list, p ->
                    list.filter { item ->
                        val matchType = p.type == null || item.transaction.type == p.type
                        val matchAmount = p.amount == null || (item.transaction.amount >= p.amount.first && item.transaction.amount <= p.amount.second)
                        matchType && matchAmount
                    }
                }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- ACTIONS ---
    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: TransactionType?) {
        _filterType.value = type
    }

    fun setDateRange(start: Long?, end: Long?) {
        if (start != null && end != null) {
            _dateRange.value = Pair(start, end)
        } else {
            _dateRange.value = null
        }
    }

    fun setAmountRange(min: Double?, max: Double?) {
        if (min != null && max != null) {
            _amountRange.value = Pair(min, max)
        } else {
            _amountRange.value = null
        }
    }

    fun saveCurrentSearch() {
        val query = _searchQuery.value.trim()
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.insertSearchHistory(query)
            }
        }
    }

    fun deleteHistoryItem(item: SearchHistoryEntity) {
        viewModelScope.launch {
            repository.deleteSearchHistory(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    // Helper data class for combining flows
    private data class SearchResultParams(
        val query: String,
        val dates: Pair<Long, Long>?,
        val type: TransactionType?,
        val amount: Pair<Double, Double>?
    )
}
