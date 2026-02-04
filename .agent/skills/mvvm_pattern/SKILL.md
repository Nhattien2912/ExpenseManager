---
name: MVVM Architecture Pattern
description: Hướng dẫn chi tiết về Model-View-ViewModel pattern trong Android với Clean Architecture.
---

# MVVM Architecture Pattern

> **Mục đích**: Skill này giải thích chi tiết về MVVM pattern, cách tổ chức code theo layers, và áp dụng trong ExpenseManager.

---

## 1. MVVM là gì?

```
┌─────────────────────────────────────────────────────────────────────┐
│                              VIEW                                    │
│   (Activity, Fragment, XML Layout)                                   │
│   - Hiển thị UI                                                      │
│   - Nhận input từ user                                               │
│   - Observe data từ ViewModel                                        │
└─────────────────────────────────────────────────────────────────────┘
                              ↕ Observe/Action
┌─────────────────────────────────────────────────────────────────────┐
│                           VIEWMODEL                                  │
│   - Giữ UI state                                                     │
│   - Xử lý business logic                                             │
│   - Expose data cho View                                             │
│   - KHÔNG biết về View (no reference to Activity/Fragment)          │
└─────────────────────────────────────────────────────────────────────┘
                              ↕ Data
┌─────────────────────────────────────────────────────────────────────┐
│                            MODEL                                     │
│   (Entity, Repository, Data Source)                                  │
│   - Data classes                                                     │
│   - Database operations                                              │
│   - Network calls                                                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Tại sao dùng MVVM?

| Vấn đề với code không có pattern | Giải pháp MVVM |
|----------------------------------|----------------|
| Activity/Fragment quá lớn (God class) | Logic chuyển sang ViewModel |
| Data mất khi xoay màn hình | ViewModel survive configuration change |
| Khó test | ViewModel có thể unit test độc lập |
| Tightly coupled | Separation of concerns |
| Callback hell | Reactive data với LiveData/StateFlow |

---

## 3. Cấu trúc thư mục MVVM

```
com.nhattien.expensemanager/
│
├── data/                          # ========== MODEL LAYER ==========
│   │
│   ├── entity/                    # Data classes (Room entities)
│   │   ├── TransactionEntity.kt   # @Entity cho bảng transactions
│   │   ├── CategoryEntity.kt      # @Entity cho bảng categories
│   │   └── DebtEntity.kt          # @Entity cho bảng debts
│   │
│   ├── dao/                       # Data Access Objects
│   │   ├── TransactionDao.kt      # CRUD operations cho transactions
│   │   ├── CategoryDao.kt
│   │   └── DebtDao.kt
│   │
│   ├── database/                  # Room Database
│   │   └── AppDatabase.kt         # @Database singleton
│   │
│   ├── repository/                # Repository pattern
│   │   ├── ExpenseRepository.kt   # Trung gian giữa ViewModel và DAO
│   │   └── CategoryRepository.kt
│   │
│   └── converter/                 # Type converters
│       └── Converters.kt          # Date, Enum conversions
│
├── domain/                        # ========== DOMAIN LAYER ==========
│   ├── TransactionType.kt         # Enum: INCOME, EXPENSE...
│   ├── FilterType.kt              # Enum: ALL, INCOME, EXPENSE
│   ├── DailySum.kt                # Data class cho aggregation
│   └── Category.kt                # Domain model
│
├── viewmodel/                     # ========== VIEWMODEL LAYER ==========
│   ├── MainViewModel.kt           # ViewModel chính
│   ├── AddTransactionViewModel.kt # ViewModel cho màn thêm giao dịch
│   ├── BudgetViewModel.kt         # ViewModel cho ngân sách
│   └── [Feature]ViewModel.kt
│
├── ui/                            # ========== VIEW LAYER ==========
│   ├── main/
│   │   ├── MainActivity.kt        # Host Activity
│   │   ├── OverviewFragment.kt    # Tab tổng quan
│   │   └── CalendarFragment.kt    # Tab lịch
│   │
│   ├── add/
│   │   └── AddTransactionActivity.kt
│   │
│   ├── adapter/                   # RecyclerView Adapters
│   │   ├── TransactionAdapter.kt
│   │   └── CategoryAdapter.kt
│   │
│   └── [feature]/
│
└── utils/                         # ========== UTILITIES ==========
    ├── CurrencyUtils.kt
    ├── DateUtils.kt
    └── BackupUtils.kt
```

---

## 4. Chi tiết từng Layer

### 4.1 MODEL Layer

```kotlin
// ========== ENTITY: Data class cho Room ==========
// File: data/entity/TransactionEntity.kt

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val type: TransactionType,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)

// ========== DAO: Database operations ==========
// File: data/dao/TransactionDao.kt

@Dao
interface TransactionDao {
    // Query trả về Flow -> reactive, auto update UI khi DB thay đổi
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    // Suspend function cho one-shot operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long
    
    @Delete
    suspend fun delete(transaction: TransactionEntity)
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?
}

// ========== REPOSITORY: Abstraction layer ==========
// File: data/repository/ExpenseRepository.kt

class ExpenseRepository(
    private val transactionDao: TransactionDao
) {
    // Expose Flow cho ViewModel
    val allTransactions: Flow<List<TransactionEntity>> = 
        transactionDao.getAllTransactions()
    
    // Wrap DAO methods
    suspend fun insert(transaction: TransactionEntity): Long {
        return transactionDao.insert(transaction)
    }
    
    suspend fun delete(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
    }
}
```

### 4.2 VIEWMODEL Layer

```kotlin
// ========== VIEWMODEL: Giữ state và logic ==========
// File: viewmodel/MainViewModel.kt

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    // ========== DEPENDENCIES ==========
    private val repository: ExpenseRepository
    
    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao())
    }
    
    // ========== UI STATE (Observable) ==========
    
    // Chuyển Flow -> StateFlow để UI observe
    val allTransactions: StateFlow<List<TransactionEntity>> = 
        repository.allTransactions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )
    
    // Computed property từ data gốc
    val totalBalance: StateFlow<Double> = allTransactions
        .map { list ->
            list.sumOf { 
                if (it.type == TransactionType.INCOME) it.amount 
                else -it.amount 
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)
    
    // Mutable state cho UI controls
    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType
    
    // ========== ACTIONS (User interactions) ==========
    
    fun setFilter(type: FilterType) {
        _filterType.value = type
    }
    
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.delete(transaction)
            // UI tự động update vì observe StateFlow
        }
    }
    
    fun insertTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insert(transaction)
        }
    }
}
```

### 4.3 VIEW Layer

```kotlin
// ========== FRAGMENT: Observe và hiển thị ==========
// File: ui/main/OverviewFragment.kt

class OverviewFragment : Fragment() {
    
    // ========== DEPENDENCIES ==========
    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!
    
    // Share ViewModel với Activity
    private val viewModel: MainViewModel by activityViewModels()
    
    private lateinit var adapter: TransactionAdapter
    
    // ========== LIFECYCLE ==========
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?, 
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeData()
    }
    
    // ========== UI SETUP ==========
    
    private fun setupUI() {
        // Adapter với click callback
        adapter = TransactionAdapter { transaction ->
            // Navigate to detail
            navigateToDetail(transaction.id)
        }
        
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Filter buttons
        binding.chipAll.setOnClickListener { 
            viewModel.setFilter(FilterType.ALL) 
        }
        binding.chipIncome.setOnClickListener { 
            viewModel.setFilter(FilterType.INCOME) 
        }
        binding.chipExpense.setOnClickListener { 
            viewModel.setFilter(FilterType.EXPENSE) 
        }
    }
    
    // ========== OBSERVE VIEWMODEL ==========
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                
                // Observe transactions
                launch {
                    viewModel.allTransactions.collect { transactions ->
                        adapter.submitList(transactions)
                        binding.emptyView.isVisible = transactions.isEmpty()
                    }
                }
                
                // Observe balance
                launch {
                    viewModel.totalBalance.collect { balance ->
                        binding.txtBalance.text = CurrencyUtils.toCurrency(balance)
                    }
                }
                
                // Observe filter
                launch {
                    viewModel.filterType.collect { type ->
                        updateFilterChips(type)
                    }
                }
            }
        }
    }
    
    // ========== CLEANUP ==========
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Tránh memory leak
    }
}
```

---

## 5. Data Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                                USER                                       │
│                          (Click, Input, Gesture)                          │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                              VIEW (Fragment)                              │
│  ┌─────────────────┐                              ┌───────────────────┐  │
│  │  User clicks    │ ──── viewModel.action() ──▶ │  Observe changes  │  │
│  │  delete button  │                              │  Update UI        │  │
│  └─────────────────┘                              └───────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
                                    │                         ▲
                                    ▼                         │
┌──────────────────────────────────────────────────────────────────────────┐
│                            VIEWMODEL                                      │
│  ┌─────────────────┐                              ┌───────────────────┐  │
│  │  deleteTransaction()                           │  StateFlow emits  │  │
│  │  viewModelScope.launch {                       │  new list         │  │
│  │    repository.delete(t)  ─────────────────────▶│                   │  │
│  │  }                                             │                   │  │
│  └─────────────────┘                              └───────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
                                    │                         ▲
                                    ▼                         │
┌──────────────────────────────────────────────────────────────────────────┐
│                            REPOSITORY                                     │
│  ┌─────────────────┐                              ┌───────────────────┐  │
│  │  suspend fun    │                              │  Flow từ DAO     │  │
│  │  delete(t) {    │ ──── dao.delete(t) ────────▶ │  auto emit khi   │  │
│  │    dao.delete() │                              │  DB thay đổi     │  │
│  │  }              │                              │                   │  │
│  └─────────────────┘                              └───────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
                                    │                         ▲
                                    ▼                         │
┌──────────────────────────────────────────────────────────────────────────┐
│                          ROOM DATABASE                                    │
│                    (SQLite với reactive updates)                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Best Practices

### ✅ NÊN làm

| Nguyên tắc | Giải thích |
|------------|------------|
| ViewModel không biết về View | Không import Activity, Fragment, View |
| Một ViewModel cho mỗi màn hình | Hoặc share nếu logic liên quan |
| Sử dụng StateFlow/LiveData | Để UI tự động update |
| Repository cho mọi data source | Dễ test, dễ thay đổi implementation |
| Coroutines trong viewModelScope | Tự cancel khi ViewModel destroyed |

### ❌ KHÔNG NÊN làm

| Anti-pattern | Vấn đề |
|--------------|--------|
| Business logic trong Activity | Khó test, khó maintain |
| ViewModel giữ reference View | Memory leak, crash |
| Gọi DB trên Main thread | ANR, UI lag |
| Hard-code strings trong code | Khó maintain, không đa ngôn ngữ |

---

## 7. Unit Testing với MVVM

```kotlin
// ========== TEST VIEWMODEL ==========
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: MainViewModel
    private val repository: ExpenseRepository = mockk()
    
    @Before
    fun setup() {
        coEvery { repository.allTransactions } returns flowOf(emptyList())
        viewModel = MainViewModel(repository)
    }
    
    @Test
    fun `delete transaction calls repository`() = runTest {
        val transaction = TransactionEntity(id = 1, amount = 100.0, ...)
        coEvery { repository.delete(any()) } just Runs
        
        viewModel.deleteTransaction(transaction)
        
        coVerify { repository.delete(transaction) }
    }
}
```

---

## 8. Quick Reference

| Component | Trách nhiệm | Ví dụ trong ExpenseManager |
|-----------|-------------|----------------------------|
| Entity | Data model | `TransactionEntity` |
| DAO | Database operations | `TransactionDao` |
| Repository | Data abstraction | `ExpenseRepository` |
| ViewModel | UI state + logic | `MainViewModel` |
| Fragment | UI display | `OverviewFragment` |
| Adapter | List rendering | `TransactionAdapter` |
