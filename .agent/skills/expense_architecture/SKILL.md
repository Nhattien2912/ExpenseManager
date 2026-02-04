---
name: ExpenseManager Architecture
description: Kiến trúc và patterns sử dụng trong dự án ExpenseManager, MVVM với StateFlow.
---

# Kiến trúc ExpenseManager

> **Mục đích**: Skill này mô tả kiến trúc tổng thể của dự án ExpenseManager, giúp đảm bảo code mới tuân thủ đúng patterns.

---

## 1. Cấu trúc thư mục

```
com.nhattien.expensemanager/
│
├── data/                      # 📦 DATA LAYER
│   ├── converter/             # Type converters cho Room (Date, Enum...)
│   ├── dao/                   # Data Access Objects (query database)
│   ├── database/              # AppDatabase singleton
│   ├── entity/                # Room entities (bảng trong DB)
│   └── repository/            # Repositories (trung gian giữa ViewModel và DAO)
│
├── domain/                    # 🎯 DOMAIN LAYER
│   ├── Category.kt            # Domain model cho Category
│   ├── TransactionType.kt     # Enum: INCOME, EXPENSE, LOAN_TAKE, LOAN_GIVE
│   ├── FilterType.kt          # Enum: ALL, INCOME, EXPENSE, RECURRING
│   ├── ChartType.kt           # Enum: PIE, BAR, LINE
│   ├── MainTab.kt             # Enum: OVERVIEW, CALENDAR, CHART
│   └── DailySum.kt            # Data class cho tổng thu/chi theo ngày
│
├── ui/                        # 🖼️ UI LAYER
│   ├── adapter/               # RecyclerView Adapters
│   ├── main/                  # MainActivity, các Fragment chính
│   ├── add/                   # AddTransactionActivity
│   ├── chart/                 # ChartFragment
│   ├── setting/               # SettingActivity
│   └── [feature]/             # Các màn hình khác
│
├── utils/                     # 🔧 UTILITIES
│   ├── CurrencyUtils.kt       # Format tiền VND/USD
│   ├── DateUtils.kt           # Xử lý ngày tháng, calendar
│   ├── BackupUtils.kt         # Export/Import JSON
│   └── TutorialHelper.kt      # Spotlight tutorial
│
└── viewmodel/                 # 🧠 VIEWMODEL LAYER
    ├── MainViewModel.kt       # ViewModel chính (13KB, nhiều logic)
    ├── AddTransactionViewModel.kt
    ├── BudgetViewModel.kt
    └── [Feature]ViewModel.kt
```

---

## 2. MVVM Pattern với StateFlow

### 2.1 Luồng dữ liệu

```
┌─────────────────────────────────────────────────────────────┐
│                         UI LAYER                            │
│  Fragment/Activity  ←───collect()───  StateFlow             │
└─────────────────────────────────────────────────────────────┘
                              ↑
                              │ stateIn()
                              │
┌─────────────────────────────────────────────────────────────┐
│                      VIEWMODEL LAYER                        │
│  ViewModel  ←───Flow───  Repository                         │
└─────────────────────────────────────────────────────────────┘
                              ↑
                              │ Flow
                              │
┌─────────────────────────────────────────────────────────────┐
│                        DATA LAYER                           │
│  Repository  ←───Flow───  DAO  ←───  Room Database          │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 ViewModel Pattern chuẩn

```kotlin
// File: viewmodel/FeatureViewModel.kt
class FeatureViewModel(application: Application) : AndroidViewModel(application) {

    // ========== KHỞI TẠO DEPENDENCIES ==========
    private val repository: ExpenseRepository
    
    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao(), db.debtDao())
    }

    // ========== REACTIVE DATA (StateFlow) ==========
    
    // Chuyển Flow từ Room thành StateFlow để UI observe
    val allTransactions = repository.allTransactions
        .stateIn(
            scope = viewModelScope,           // Tự cancel khi ViewModel destroyed
            started = SharingStarted.Lazily,  // Chỉ start khi có collector
            initialValue = emptyList()        // Giá trị ban đầu
        )
    
    // ========== COMPUTED PROPERTIES ==========
    
    // Tính toán từ data gốc bằng .map()
    val totalBalance = allTransactions.map { list ->
        list.sumOf { 
            if (it.transaction.type == TransactionType.INCOME) 
                it.transaction.amount 
            else 
                -it.transaction.amount 
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)
    
    // ========== UI STATE ==========
    
    // MutableStateFlow cho state có thể thay đổi từ UI
    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate
    
    fun setSelectedDate(date: Calendar) {
        _selectedDate.value = date
    }

    // ========== ACTIONS ==========
    
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            // StateFlow tự động update UI vì Room emit Flow mới
        }
    }
}
```

---

## 3. Repository Pattern

```kotlin
// File: data/repository/ExpenseRepository.kt
class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao
) {
    // ========== REACTIVE QUERIES (Flow) ==========
    
    // Room tự động emit data mới khi DB thay đổi
    val allTransactions: Flow<List<TransactionWithCategory>> = 
        transactionDao.getAllTransactionsWithCategory()
    
    // ========== SUSPEND FUNCTIONS (one-shot) ==========
    
    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insert(transaction)
    }
    
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
    }
    
    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getById(id)
    }
}
```

---

## 4. Collect StateFlow trong Fragment

```kotlin
// File: ui/main/OverviewFragment.kt
class OverviewFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ========== OBSERVE STATEFLOW ==========
        
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle đảm bảo chỉ collect khi Fragment STARTED
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                // Có thể launch nhiều collectors song song
                launch {
                    viewModel.allTransactions.collect { transactions ->
                        adapter.submitList(transactions)
                    }
                }
                
                launch {
                    viewModel.totalBalance.collect { balance ->
                        binding.txtBalance.text = CurrencyUtils.toCurrency(balance)
                    }
                }
            }
        }
    }
}
```

---

## 5. Combine nhiều Flows

```kotlin
// Trong MainViewModel - kết hợp nhiều nguồn data
val recentTransactions = combine(
    allTransactions,    // Flow 1
    _selectedDate,      // Flow 2
    filterType,         // Flow 3
    _viewMode           // Flow 4
) { list, date, type, mode ->
    
    // Logic filter theo tất cả các tham số
    val filteredByTime = if (mode == ViewMode.DAILY) {
        list.filter { /* filter theo ngày */ }
    } else {
        list.filter { /* filter theo tháng */ }
    }
    
    when (type) {
        FilterType.ALL -> filteredByTime
        FilterType.INCOME -> filteredByTime.filter { it.transaction.type == TransactionType.INCOME }
        FilterType.EXPENSE -> filteredByTime.filter { it.transaction.type == TransactionType.EXPENSE }
    }
}.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```

---

## 6. Best Practices trong dự án

| Nguyên tắc | Mô tả |
|------------|-------|
| **AndroidViewModel** | Sử dụng để access Application context an toàn |
| **StateFlow + stateIn()** | Thay vì LiveData, reactive hơn |
| **combine()** | Merge nhiều flows khi cần |
| **Repository layer** | Trung gian giữa ViewModel và Data source |
| **ViewBinding** | Tất cả UI đều dùng ViewBinding |
| **Coroutines** | Tất cả DB operations đều là suspend functions |

---

## 7. Khi thêm tính năng mới

1. **Entity** → Thêm vào `data/entity/`
2. **DAO** → Thêm queries vào `data/dao/`
3. **Repository** → Wrap DAO trong Repository
4. **ViewModel** → Expose StateFlow cho UI
5. **Fragment/Activity** → Collect và hiển thị
