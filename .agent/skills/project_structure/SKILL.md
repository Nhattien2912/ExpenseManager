---
name: Kotlin Project Structure
description: Hướng dẫn tổ chức cấu trúc thư mục và file trong dự án Android Kotlin.
---

# Kotlin Project Structure

> **Mục đích**: Skill này hướng dẫn cách tổ chức thư mục, đặt tên file, và phân chia code theo conventions chuẩn.

---

## 1. Cấu trúc dự án Android chuẩn

```
ExpenseManager/                          # Root project
│
├── .agent/                              # Agent skills (AI assistant)
│   └── skills/
│
├── app/                                 # Main application module
│   ├── build.gradle.kts                 # Dependencies, SDK config
│   ├── proguard-rules.pro               # Code obfuscation rules
│   │
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # App configuration
│       │   │
│       │   ├── java/                    # Kotlin/Java source code
│       │   │   └── com/nhattien/expensemanager/
│       │   │       ├── data/            # Data layer
│       │   │       ├── domain/          # Domain layer
│       │   │       ├── ui/              # UI layer
│       │   │       ├── viewmodel/       # ViewModels
│       │   │       └── utils/           # Utilities
│       │   │
│       │   └── res/                     # Resources
│       │       ├── layout/              # XML layouts
│       │       ├── drawable/            # Images, icons, shapes
│       │       ├── values/              # Strings, colors, styles
│       │       ├── menu/                # Menu definitions
│       │       └── navigation/          # Navigation graphs
│       │
│       ├── test/                        # Unit tests
│       │   └── java/
│       │
│       └── androidTest/                 # Instrumented tests
│           └── java/
│
├── gradle/                              # Gradle wrapper
├── build.gradle.kts                     # Project-level build config
├── settings.gradle.kts                  # Module settings
└── gradle.properties                    # Gradle properties
```

---

## 2. Source Code Organization (by Feature)

### 2.1 Package by Layer (Hiện tại của ExpenseManager)

```
com.nhattien.expensemanager/
├── data/                    # Tất cả data-related code
│   ├── entity/
│   ├── dao/
│   ├── database/
│   └── repository/
├── domain/                  # Business models
├── ui/                      # Tất cả UI code
│   ├── main/
│   ├── add/
│   └── adapter/
├── viewmodel/              # Tất cả ViewModels
└── utils/                  # Utilities
```

### 2.2 Package by Feature (Recommended cho dự án lớn)

```
com.nhattien.expensemanager/
├── core/                    # Shared code
│   ├── database/
│   ├── utils/
│   └── base/                # Base classes
│
├── feature/
│   ├── transaction/         # Feature: Transactions
│   │   ├── data/
│   │   │   ├── TransactionEntity.kt
│   │   │   ├── TransactionDao.kt
│   │   │   └── TransactionRepository.kt
│   │   ├── ui/
│   │   │   ├── TransactionListFragment.kt
│   │   │   └── AddTransactionActivity.kt
│   │   └── TransactionViewModel.kt
│   │
│   ├── category/            # Feature: Categories
│   │   ├── data/
│   │   ├── ui/
│   │   └── CategoryViewModel.kt
│   │
│   └── budget/              # Feature: Budget
│       ├── data/
│       ├── ui/
│       └── BudgetViewModel.kt
│
└── ExpenseManagerApp.kt     # Application class
```

---

## 3. Quy tắc đặt tên File

### 3.1 Kotlin Files

| Loại | Pattern | Ví dụ |
|------|---------|-------|
| Activity | `[Name]Activity.kt` | `AddTransactionActivity.kt` |
| Fragment | `[Name]Fragment.kt` | `OverviewFragment.kt` |
| ViewModel | `[Name]ViewModel.kt` | `MainViewModel.kt` |
| Adapter | `[Name]Adapter.kt` | `TransactionAdapter.kt` |
| Entity | `[Name]Entity.kt` | `TransactionEntity.kt` |
| DAO | `[Name]Dao.kt` | `TransactionDao.kt` |
| Repository | `[Name]Repository.kt` | `ExpenseRepository.kt` |
| Utils | `[Name]Utils.kt` | `CurrencyUtils.kt` |
| Factory | `[Name]Factory.kt` | `ViewModelFactory.kt` |

### 3.2 XML Resources

| Loại | Pattern | Ví dụ |
|------|---------|-------|
| Activity layout | `activity_[name].xml` | `activity_add_transaction.xml` |
| Fragment layout | `fragment_[name].xml` | `fragment_overview.xml` |
| List item | `item_[name].xml` | `item_transaction.xml` |
| Dialog | `dialog_[name].xml` | `dialog_confirm_delete.xml` |
| Include/Partial | `partial_[name].xml` | `partial_balance_card.xml` |
| Menu | `menu_[name].xml` | `menu_main.xml` |
| Bottom nav | `bottom_nav_menu.xml` | `bottom_nav_menu.xml` |
| Drawable | `ic_[name].xml` / `bg_[name].xml` | `ic_add.xml`, `bg_card.xml` |

### 3.3 Resource IDs (trong XML)

```xml
<!-- Pattern: [type]_[description] hoặc [type][Name] (camelCase) -->

<!-- Views -->
<TextView android:id="@+id/txtBalance" />
<TextView android:id="@+id/txtAmount" />
<EditText android:id="@+id/edtNote" />
<Button android:id="@+id/btnSave" />
<ImageView android:id="@+id/imgCategory" />
<RecyclerView android:id="@+id/recyclerTransactions" />

<!-- Cards, Containers -->
<CardView android:id="@+id/cardBalance" />
<LinearLayout android:id="@+id/containerFilters" />

<!-- Chips, Tabs -->
<Chip android:id="@+id/chipAll" />
<TabLayout android:id="@+id/tabLayout" />
```

---

## 4. Class Organization

### 4.1 Thứ tự trong một file Kotlin

```kotlin
// 1. Package declaration
package com.nhattien.expensemanager.viewmodel

// 2. Imports (sorted alphabetically, grouped)
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.StateFlow

// 3. Class declaration
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    // ========== COMPANION OBJECT (constants, factory) ==========
    companion object {
        private const val TAG = "MainViewModel"
        const val DEFAULT_LIMIT = 5000000.0
    }
    
    // ========== PRIVATE PROPERTIES ==========
    private val repository: ExpenseRepository
    private val _isLoading = MutableStateFlow(false)
    
    // ========== PUBLIC PROPERTIES (read-only exposure) ==========
    val isLoading: StateFlow<Boolean> = _isLoading
    val allTransactions = repository.allTransactions
    
    // ========== INIT BLOCK ==========
    init {
        val db = AppDatabase.getInstance(application)
        repository = ExpenseRepository(db.transactionDao())
    }
    
    // ========== PUBLIC METHODS ==========
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            // ...
            _isLoading.value = false
        }
    }
    
    fun deleteTransaction(transaction: TransactionEntity) {
        // ...
    }
    
    // ========== PRIVATE METHODS ==========
    private fun calculateBalance(list: List<Transaction>): Double {
        // ...
    }
}
```

### 4.2 Fragment/Activity Organization

```kotlin
class OverviewFragment : Fragment() {
    
    // ========== BINDING & VIEWMODEL ==========
    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by activityViewModels()
    
    // ========== ADAPTERS ==========
    private lateinit var transactionAdapter: TransactionAdapter
    
    // ========== LIFECYCLE ==========
    override fun onCreateView(...): View { }
    
    override fun onViewCreated(...) { }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // ========== UI SETUP ==========
    private fun setupRecyclerView() { }
    
    private fun setupClickListeners() { }
    
    // ========== OBSERVERS ==========
    private fun observeData() { }
    
    // ========== NAVIGATION ==========
    private fun navigateToDetail(id: Long) { }
    
    private fun navigateToAddTransaction() { }
    
    // ========== HELPER METHODS ==========
    private fun showError(message: String) { }
    
    private fun showLoading(show: Boolean) { }
}
```

---

## 5. Resource Organization

### 5.1 values/

```
values/
├── colors.xml          # Color definitions
├── strings.xml         # Text strings (main language)
├── styles.xml          # App theme và styles
├── themes.xml          # Material theme
├── dimens.xml          # Dimensions (margins, padding)
└── attrs.xml           # Custom attributes
```

### 5.2 Layout Guidelines

```xml
<!-- activity_main.xml -->
<?xml version="1.0" encoding="utf-8"?>
<!-- Root: thường là ConstraintLayout hoặc CoordinatorLayout -->
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".ui.main.MainActivity">
    
    <!-- Toolbar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        ... />
    
    <!-- Main Content -->
    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/navHostFragment"
        ... />
    
    <!-- Bottom Navigation -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottomNav"
        ... />
    
    <!-- FAB -->
    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fabAdd"
        ... />
        
</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 6. Gradle Module Structure

### 6.1 Single Module (Hiện tại)

```
project/
├── app/                     # Tất cả code trong 1 module
│   └── build.gradle.kts
└── build.gradle.kts         # Project-level
```

### 6.2 Multi-Module (Recommended cho dự án lớn)

```
project/
├── app/                     # Application module
│   └── build.gradle.kts
│
├── core/                    # Shared core module
│   ├── core-data/           # Database, network
│   ├── core-ui/             # Base UI components
│   └── core-utils/          # Utilities
│
├── feature/                 # Feature modules
│   ├── feature-transaction/
│   ├── feature-budget/
│   └── feature-settings/
│
└── build.gradle.kts
```

---

## 7. Quick Reference

| Loại file | Thư mục | Naming |
|-----------|---------|--------|
| Entity | `data/entity/` | `[Name]Entity.kt` |
| DAO | `data/dao/` | `[Name]Dao.kt` |
| Repository | `data/repository/` | `[Name]Repository.kt` |
| ViewModel | `viewmodel/` | `[Name]ViewModel.kt` |
| Activity | `ui/[feature]/` | `[Name]Activity.kt` |
| Fragment | `ui/[feature]/` | `[Name]Fragment.kt` |
| Adapter | `ui/adapter/` | `[Name]Adapter.kt` |
| Layout | `res/layout/` | `[type]_[name].xml` |
| Drawable | `res/drawable/` | `ic_` or `bg_[name].xml` |
