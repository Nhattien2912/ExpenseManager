package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.domain.TransactionType
import com.nhattien.expensemanager.domain.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DailySum(val income: Double = 0.0, val expense: Double = 0.0)

enum class FilterType { ALL, INCOME, EXPENSE }
enum class MainTab { OVERVIEW, REPORT }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val currentCalendar = MutableStateFlow(Calendar.getInstance())

    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
    }

    val allTransactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalBalance = allTransactions.map { list ->
        list.sumOf { if (it.type == TransactionType.INCOME || it.type == TransactionType.LOAN_TAKE) it.amount else -it.amount }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val monthlyStats = combine(allTransactions, currentCalendar) { list, cal ->
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        
        val filtered = list.filter { 
            val itemCal = Calendar.getInstance().apply { timeInMillis = it.date }
            itemCal.get(Calendar.MONTH) == month && itemCal.get(Calendar.YEAR) == year
        }

        val income = filtered.filter { it.type == TransactionType.INCOME || it.type == TransactionType.LOAN_TAKE }.sumOf { it.amount }
        val expense = filtered.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LOAN_GIVE }.sumOf { it.amount }
        
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

    val filterType = MutableStateFlow(FilterType.ALL) // Enum define below or use Int
    
    
    // View Mode: Daily or Monthly
    enum class ViewMode { DAILY, MONTHLY }
    
    private val _viewMode = MutableStateFlow(ViewMode.DAILY)
    val viewMode = _viewMode

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    // Filter Transactions by Selected Date OR Month depending on ViewMode
    val recentTransactions = combine(allTransactions, _selectedDate, filterType, _viewMode, currentCalendar) { list, date, type, mode, monthCal ->
        
        val filteredByTime = if (mode == ViewMode.DAILY) {
            val selectedDay = date.get(Calendar.DAY_OF_YEAR)
            val selectedYear = date.get(Calendar.YEAR)
            list.filter { 
                val itemCal = Calendar.getInstance().apply { timeInMillis = it.date }
                itemCal.get(Calendar.DAY_OF_YEAR) == selectedDay && itemCal.get(Calendar.YEAR) == selectedYear
            }
        } else {
            // MONTHLY MODE
            val selectedMonth = monthCal.get(Calendar.MONTH)
            val selectedYear = monthCal.get(Calendar.YEAR)
            list.filter {
                val itemCal = Calendar.getInstance().apply { timeInMillis = it.date }
                itemCal.get(Calendar.MONTH) == selectedMonth && itemCal.get(Calendar.YEAR) == selectedYear
            }
        }

        val sorted = filteredByTime.sortedByDescending { it.date }
        
        when (type) {
            FilterType.ALL -> sorted
            FilterType.INCOME -> sorted.filter { it.type == TransactionType.INCOME || it.type == TransactionType.LOAN_TAKE }
            FilterType.EXPENSE -> sorted.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LOAN_GIVE }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setFilter(type: FilterType) {
        filterType.value = type
    }

    // NÂNG CẤP: Tính riêng Thu và Chi cho từng ngày
    // NÂNG CẤP: Tính riêng Thu và Chi cho từng ngày
    val calendarDailyTotals = monthlyStats.map { (_, _, list) ->
        val map = mutableMapOf<Int, DailySum>()
        list.forEach { 
            val day = Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
            val current = map[day] ?: DailySum()
            
            if (it.type == TransactionType.INCOME || it.type == TransactionType.LOAN_TAKE) {
                map[day] = current.copy(income = current.income + it.amount)
            } else if (it.type == TransactionType.EXPENSE || it.type == TransactionType.LOAN_GIVE) {
                map[day] = current.copy(expense = current.expense + it.amount)
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // New: Calculate Today's Balance
    val todayBalance = calendarDailyTotals.map { map ->
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val sum = map[today] ?: DailySum()
        sum.income - sum.expense
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val categoryDistribution = monthlyStats.map { (_, _, list) ->
        val expensesOnly = list.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LOAN_GIVE }
        val totalExp = expensesOnly.sumOf { it.amount }
        if (totalExp == 0.0) return@map emptyMap<Category, Double>()
        
        expensesOnly.groupBy { it.category }
            .mapValues { (it.value.sumOf { trans -> trans.amount } / totalExp) * 100 }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // --- Spending Limit Logic (Moved from BudgetViewModel) ---
    private val prefs = application.getSharedPreferences("expense_manager", android.content.Context.MODE_PRIVATE)
    private val _spendingLimit = MutableStateFlow(prefs.getFloat("KEY_SPENDING_LIMIT", 5000000f).toDouble())
    val spendingLimit = _spendingLimit

    val currentMonthExpense = monthlyStats.map { (_, expense, _) -> expense } // Reuse calculated monthly expense
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    fun setSpendingLimit(amount: Double) {
        prefs.edit().putFloat("KEY_SPENDING_LIMIT", amount.toFloat()).apply()
        _spendingLimit.value = amount
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
        list.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.LOAN_GIVE }
            .forEach {
                val day = Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
                dailyMap[day] = (dailyMap[day] ?: 0.0) + it.amount
            }
        dailyMap
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Balance Trend for Line Chart (Day -> Cumulative Balance)
    val balanceTrendData = combine(allTransactions, currentCalendar) { all, cal ->
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        
        // 1. Calculate Opening Balance (Everything before the 1st of this month)
        val startHofMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val initialBalance = all.filter { it.date < startHofMonth }
            .sumOf { if (it.type == TransactionType.INCOME || it.type == TransactionType.LOAN_TAKE) it.amount else -it.amount }
            
        // 2. Get transactions of this month
        val monthTrans = all.filter { 
            val c = Calendar.getInstance().apply { timeInMillis = it.date }
            c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear
        }.sortedBy { it.date }
        
        // 3. Compute running balance
        val points = mutableListOf<Pair<Int, Double>>()
        var currentBal = initialBalance
        
        // Need to output a value for every day? Or just days with transactions?
        // Line chart looks better if we have points for every day or relevant changes.
        // Let's accumulate by day.
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val transByDay = monthTrans.groupBy { 
            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH) 
        }
        
        for (day in 1..daysInMonth) {
            val dailyNet = transByDay[day]?.sumOf { 
                 if (it.type == TransactionType.INCOME || it.type == TransactionType.LOAN_TAKE) it.amount else -it.amount 
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
}

enum class ChartType { PIE, BAR, LINE }

// Extension to map DailySum to BarEntry/Entry if needed, 
// but Logic is inside ViewModel to keep UI clean.
// Actually, MPAndroidChart Entry needs floats/ints. 
// I'll return List<com.github.mikephil.charting.data.Entry> and BarEntry.
// But wait, I shouldn't depend on MPAndroidChart types in ViewModel if possible to keep it pure?
// It's acceptable for this scope to have simple data classes or mapped inside Fragment.
// Let's map inside ViewModel for simplicity as 'Entry' is data-holder.
// Need to add MPAndroidChart dependency to ViewModel or just return raw Map and map in Fragment?
// Mapping in Fragment is cleaner for Architecture (ViewModel returns Domain/Data, UI maps to View specific types).
// But for speed, I'll return lists of Pair<Int, Double> or custom data class?
// No, the plan said "dailyExpense: StateFlow<List<BarEntry>>".
// I will assume MPAndroidChart is available and used here or I will return raw data.
// Let's return raw data maps/lists and let Fragment convert to Entry. 
// Actually, I already return `calendarDailyTotals` which is Map<Int, DailySum>.
// For Bar Chart: I need List of (Day, Expense). 
// For Line Chart: I need List of (Day, Cumulative Balance).

// Revise plan: Expose flow of data, map to Entry in Fragment.
// dailyExpenseData: Map<Int, Double> (Day -> Expense)
// balanceTrendData: Map<Int, Double> (Day -> End of Day Balance)

// Wait, calculating "Balance Trend" is tricky. It depends on opening balance of the month.
// I have `allTransactions`.
// To get balance trend for current month:
// 1. Calculate opening balance before this month.
// 2. Iterate days of this month, adding income/expense.
// This is heavy computation? 
// Simplified Line Chart: "Daily Net Flow" or "Running Balance from Day 1 of Month"?
// User likely wants "Account Balance" evolution.
// Let's do: "Balance trend within this month".
// Start = Balance up to End of Previous Month.
// Points = Start + Cumulative Sum of (Inc - Exp) for each day.

// I will add `chartType` flow in ViewModel first.

