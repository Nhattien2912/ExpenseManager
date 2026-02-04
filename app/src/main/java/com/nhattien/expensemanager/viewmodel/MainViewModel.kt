package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.CategoryRepository
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.domain.ChartType
import com.nhattien.expensemanager.domain.DailySum
import com.nhattien.expensemanager.domain.FilterType
import com.nhattien.expensemanager.domain.MainTab
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val categoryRepository: CategoryRepository 
    private val currentCalendar = MutableStateFlow(Calendar.getInstance())

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
        categoryRepository = CategoryRepository(db.categoryDao()) // Init
    }

    val allTransactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Expose Categories for UI
    val allCategories = kotlinx.coroutines.flow.flow { 
        emit(categoryRepository.getAllCategories()) 
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalBalance = allTransactions.map { list ->
        list.sumOf { 
            // Access via .transaction
            if (it.transaction.type == TransactionType.INCOME || it.transaction.type == TransactionType.LOAN_TAKE) 
                it.transaction.amount 
            else 
                -it.transaction.amount 
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val monthlyStats = combine(allTransactions, currentCalendar) { list, cal ->
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        
        val filtered = list.filter { 
            val itemCal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            itemCal.get(Calendar.MONTH) == month && itemCal.get(Calendar.YEAR) == year
        }

        val income = filtered.filter { it.transaction.type == TransactionType.INCOME || it.transaction.type == TransactionType.LOAN_TAKE }.sumOf { it.transaction.amount }
        val expense = filtered.filter { it.transaction.type == TransactionType.EXPENSE || it.transaction.type == TransactionType.LOAN_GIVE }.sumOf { it.transaction.amount }
        
        Triple(income, expense, filtered)
    }.stateIn(viewModelScope, SharingStarted.Lazily, Triple(0.0, 0.0, emptyList()))

    val monthlyIncome = monthlyStats.map { it.first }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)
    val monthlyExpense = monthlyStats.map { it.second }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // Date Selection for Transaction List
    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate = _selectedDate

    fun setSelectedDate(cal: Calendar) {
        _selectedDate.value = cal
    }

    val filterType = MutableStateFlow(FilterType.ALL)
    
    // View Mode: Daily or Monthly
    enum class ViewMode { DAILY, MONTHLY }
    
    private val _viewMode = MutableStateFlow(ViewMode.DAILY)
    val viewMode = _viewMode

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    // Filter Transactions by Selected Date OR Month
    val recentTransactions = combine(allTransactions, _selectedDate, filterType, _viewMode, currentCalendar) { list, date, type, mode, monthCal ->
        
        val filteredByTime = if (mode == ViewMode.DAILY) {
            val selectedDay = date.get(Calendar.DAY_OF_YEAR)
            val selectedYear = date.get(Calendar.YEAR)
            list.filter { 
                val itemCal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
                itemCal.get(Calendar.DAY_OF_YEAR) == selectedDay && itemCal.get(Calendar.YEAR) == selectedYear
            }
        } else {
            // MONTHLY MODE
            val selectedMonth = monthCal.get(Calendar.MONTH)
            val selectedYear = monthCal.get(Calendar.YEAR)
            list.filter {
                val itemCal = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
                itemCal.get(Calendar.MONTH) == selectedMonth && itemCal.get(Calendar.YEAR) == selectedYear
            }
        }

        val sorted = filteredByTime.sortedByDescending { it.transaction.date }
        
        when (type) {
            FilterType.ALL -> sorted
            FilterType.INCOME -> sorted.filter { it.transaction.type == TransactionType.INCOME || it.transaction.type == TransactionType.LOAN_TAKE }
            FilterType.EXPENSE -> sorted.filter { it.transaction.type == TransactionType.EXPENSE || it.transaction.type == TransactionType.LOAN_GIVE }
            FilterType.RECURRING -> sorted.filter { it.transaction.isRecurring }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilter(type: FilterType) {
        filterType.value = type
    }

    // Daily totals
    val calendarDailyTotals = monthlyStats.map { (_, _, list) ->
        val map = mutableMapOf<Int, DailySum>()
        list.forEach { 
            val day = Calendar.getInstance().apply { timeInMillis = it.transaction.date }.get(Calendar.DAY_OF_MONTH)
            val current = map[day] ?: DailySum()
            
            if (it.transaction.type == TransactionType.INCOME || it.transaction.type == TransactionType.LOAN_TAKE) {
                map[day] = current.copy(income = current.income + it.transaction.amount)
            } else if (it.transaction.type == TransactionType.EXPENSE || it.transaction.type == TransactionType.LOAN_GIVE) {
                map[day] = current.copy(expense = current.expense + it.transaction.amount)
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Today's Balance
    val todayBalance = calendarDailyTotals.map { map ->
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val sum = map[today] ?: DailySum()
        sum.income - sum.expense
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val categoryDistribution = monthlyStats.map { (_, _, list) ->
        val expensesOnly = list.filter { it.transaction.type == TransactionType.EXPENSE }
        val totalExp = expensesOnly.sumOf { it.transaction.amount }
        if (totalExp == 0.0) return@map emptyMap<CategoryEntity, Double>() // Key is now CategoryEntity
        
        expensesOnly.groupBy { it.category }
            .mapValues { (it.value.sumOf { trans -> trans.transaction.amount } / totalExp) * 100 }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // --- Spending Limit Logic (Moved from BudgetViewModel) ---
    private val prefs = application.getSharedPreferences("expense_manager", android.content.Context.MODE_PRIVATE)
    private val _spendingLimit = MutableStateFlow(prefs.getFloat("KEY_SPENDING_LIMIT", 5000000f).toDouble())
    val spendingLimit = _spendingLimit
    
    // Listener to sync spending limit from other ViewModels
    private val prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "KEY_SPENDING_LIMIT") {
            _spendingLimit.value = prefs.getFloat("KEY_SPENDING_LIMIT", 5000000f).toDouble()
        }
    }

    val currentMonthExpense = monthlyStats.map { (_, expense, _) -> expense } // Reuse calculated monthly expense
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    fun setSpendingLimit(amount: Double) {
        prefs.edit().putFloat("KEY_SPENDING_LIMIT", amount.toFloat()).apply()
        _spendingLimit.value = amount
    }
    
    fun registerPrefsListener() {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }
    
    fun unregisterPrefsListener() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    // --- Tab Logic ---
    private val _currentTab = MutableStateFlow(MainTab.OVERVIEW)
    val currentTab = _currentTab

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }
    
    // --- Chart Logic ---
    private val _chartType = MutableStateFlow(ChartType.PIE)
    val chartType = _chartType

    fun setChartType(type: ChartType) {
        _chartType.value = type
    }
    
    // Daily Expense for Bar Chart (Day -> Amount)
    val dailyExpenseData = monthlyStats.map { (_, _, list) ->
        val dailyMap = mutableMapOf<Int, Double>()
        list.filter { it.transaction.type == TransactionType.EXPENSE || it.transaction.type == TransactionType.LOAN_GIVE }
            .forEach {
                val day = Calendar.getInstance().apply { timeInMillis = it.transaction.date }.get(Calendar.DAY_OF_MONTH)
                dailyMap[day] = (dailyMap[day] ?: 0.0) + it.transaction.amount
            }
        dailyMap
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Balance Trend for Line Chart
    val balanceTrendData = combine(allTransactions, currentCalendar) { all, cal ->
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        
        val startHofMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val initialBalance = all.filter { it.transaction.date < startHofMonth }
            .sumOf { 
                if (it.transaction.type == TransactionType.INCOME || it.transaction.type == TransactionType.LOAN_TAKE) 
                    it.transaction.amount 
                else 
                    -it.transaction.amount 
            }
            
        val monthTrans = all.filter { 
            val c = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }.sortedBy { it.transaction.date }
        
        val points = mutableListOf<Pair<Int, Double>>()
        var currentBal = initialBalance
        
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val transByDay = monthTrans.groupBy { 
            Calendar.getInstance().apply { timeInMillis = it.transaction.date }.get(Calendar.DAY_OF_MONTH) 
        }
        
        for (day in 1..daysInMonth) {
            val dailyNet = transByDay[day]?.sumOf { 
                 if (it.transaction.type == TransactionType.INCOME || it.transaction.type == TransactionType.LOAN_TAKE) 
                    it.transaction.amount 
                 else 
                    -it.transaction.amount 
            } ?: 0.0
            currentBal += dailyNet
            points.add(day to currentBal)
        }
        points
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun changeMonth(offset: Int) {
        val newCal = Calendar.getInstance().apply { 
            timeInMillis = currentCalendar.value.timeInMillis
            add(Calendar.MONTH, offset) 
        }
        currentCalendar.value = newCal
    }

    fun setCurrentMonth(year: Int, month: Int) {
        val newCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        currentCalendar.value = newCal
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
    fun exportData(context: android.content.Context): android.net.Uri? {
        // Run blocking for simplicity in this demo, or use coroutine returning result
        // For ViewModels, usually we don't pass Context. But for generating file in CacheDir it's needed.
        // Ideally Repository should handle this, but for quick implementation we do it here or in Fragment.
        // Actually, CsvUtils requires Context. Let's make this simple: The Fragment will call ViewModel to get List, then Fragment calls CsvUtils.
        // BETTER APPROACH: Fragment observes allTransactions, then on Export Click:
        // val list = viewModel.allTransactions.value
        // val uri = CsvUtils.exportTransactionsToCsv(requireContext(), list)
        return null
    }
}



