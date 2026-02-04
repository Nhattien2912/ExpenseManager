---
name: ExpenseManager Transactions
description: Cách làm việc với Transaction trong ExpenseManager - Entity, DAO, và hiển thị.
---

# Transactions trong ExpenseManager

> **Mục đích**: Skill này hướng dẫn cách làm việc với Transaction - entity chính của ứng dụng quản lý chi tiêu.

---

## 1. TransactionEntity

```kotlin
// File: data/entity/TransactionEntity.kt
// Mô tả: Entity đại diện cho một giao dịch thu/chi

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                              // ID tự tăng
    
    val amount: Double = 0.0,                      // Số tiền (luôn dương)
    
    val categoryId: Long,                          // FK đến CategoryEntity
    
    val paymentMethod: String = "CASH",            // "CASH" hoặc "BANK"
    
    val type: TransactionType = TransactionType.EXPENSE,  // Loại giao dịch
    
    val note: String = "",                         // Ghi chú
    
    val date: Long = System.currentTimeMillis(),   // Timestamp (milliseconds)
    
    val isRecurring: Boolean = false,              // Giao dịch lặp lại
    
    val debtId: Long? = null                       // Liên kết với khoản nợ (nếu có)
)
```

---

## 2. TransactionType Enum

```kotlin
// File: domain/TransactionType.kt
// Mô tả: Các loại giao dịch trong ứng dụng

enum class TransactionType {
    INCOME,      // Thu nhập: lương, thưởng, bán hàng...
    EXPENSE,     // Chi tiêu: ăn uống, mua sắm, hóa đơn...
    LOAN_TAKE,   // Vay tiền: nhận tiền từ người khác
    LOAN_GIVE    // Cho vay: đưa tiền cho người khác
}

// Cách kiểm tra loại:
when (transaction.type) {
    TransactionType.INCOME, TransactionType.LOAN_TAKE -> {
        // Tiền VÀO (balance tăng)
    }
    TransactionType.EXPENSE, TransactionType.LOAN_GIVE -> {
        // Tiền RA (balance giảm)
    }
}
```

---

## 3. TransactionWithCategory (JOIN result)

```kotlin
// File: data/entity/TransactionWithCategory.kt
// Mô tả: Wrapper chứa Transaction kèm Category (kết quả từ Room @Relation)

data class TransactionWithCategory(
    @Embedded 
    val transaction: TransactionEntity,    // Full transaction data
    
    @Relation(
        parentColumn = "categoryId",       // FK trong TransactionEntity
        entityColumn = "id"                // PK trong CategoryEntity
    )
    val category: CategoryEntity           // Category tương ứng
)

// Sử dụng trong code:
val item: TransactionWithCategory = ...
val amount = item.transaction.amount       // Truy cập transaction
val categoryName = item.category.name      // Truy cập category
val categoryIcon = item.category.icon      // Emoji của category
```

---

## 4. Tạo Transaction mới

```kotlin
// Trong AddTransactionActivity hoặc ViewModel

// Bước 1: Thu thập dữ liệu từ UI
val amount = CurrencyUtils.parseFromSeparator(binding.edtAmount.text.toString())
val note = binding.edtNote.text.toString()
val selectedCategory = viewModel.selectedCategory.value
val isBank = binding.rbBank.isChecked

// Bước 2: Tạo entity
val transaction = TransactionEntity(
    amount = amount,
    categoryId = selectedCategory?.id ?: 1L,
    paymentMethod = if (isBank) "BANK" else "CASH",
    type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
    note = note,
    date = selectedDate.timeInMillis,  // Hoặc System.currentTimeMillis()
    isRecurring = binding.switchRecurring.isChecked
)

// Bước 3: Insert vào database
viewModel.insertTransaction(transaction)
```

---

## 5. Filter Transactions

```kotlin
// Trong MainViewModel
// Mô tả: Lọc transactions theo nhiều tiêu chí

val recentTransactions = combine(
    allTransactions,      // Tất cả transactions từ DB
    _selectedDate,        // Ngày đang chọn
    filterType,           // Loại filter (ALL, INCOME, EXPENSE)
    _viewMode             // Chế độ xem (DAILY, MONTHLY)
) { list, date, type, mode ->
    
    // ========== BƯỚC 1: Lọc theo thời gian ==========
    val filteredByTime = if (mode == ViewMode.DAILY) {
        // Lọc theo ngày cụ thể
        val selectedDay = date.get(Calendar.DAY_OF_YEAR)
        val selectedYear = date.get(Calendar.YEAR)
        
        list.filter { 
            val itemCal = Calendar.getInstance().apply { 
                timeInMillis = it.transaction.date 
            }
            itemCal.get(Calendar.DAY_OF_YEAR) == selectedDay && 
            itemCal.get(Calendar.YEAR) == selectedYear
        }
    } else {
        // Lọc theo tháng
        val selectedMonth = date.get(Calendar.MONTH)
        val selectedYear = date.get(Calendar.YEAR)
        
        list.filter {
            val itemCal = Calendar.getInstance().apply { 
                timeInMillis = it.transaction.date 
            }
            itemCal.get(Calendar.MONTH) == selectedMonth && 
            itemCal.get(Calendar.YEAR) == selectedYear
        }
    }
    
    // ========== BƯỚC 2: Sắp xếp theo ngày (mới nhất trước) ==========
    val sorted = filteredByTime.sortedByDescending { it.transaction.date }
    
    // ========== BƯỚC 3: Lọc theo loại ==========
    when (type) {
        FilterType.ALL -> sorted
        FilterType.INCOME -> sorted.filter { 
            it.transaction.type == TransactionType.INCOME || 
            it.transaction.type == TransactionType.LOAN_TAKE 
        }
        FilterType.EXPENSE -> sorted.filter { 
            it.transaction.type == TransactionType.EXPENSE || 
            it.transaction.type == TransactionType.LOAN_GIVE 
        }
        FilterType.RECURRING -> sorted.filter { it.transaction.isRecurring }
    }
}.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```

---

## 6. Hiển thị trong RecyclerView

```kotlin
// File: ui/adapter/TransactionAdapter.kt

class TransactionAdapter(
    private val onItemClick: (TransactionEntity) -> Unit
) : ListAdapter<TransactionWithCategory, TransactionAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemTransactionBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: TransactionWithCategory) {
            val transaction = item.transaction
            val category = item.category
            
            // ========== HIỂN THỊ CATEGORY ==========
            binding.txtCategoryIcon.text = category.icon    // Emoji: 🍔
            binding.txtTitle.text = category.name           // "Ăn uống"
            
            // ========== HIỂN THỊ GHI CHÚ ==========
            binding.txtNote.text = transaction.note.ifEmpty { "Không có ghi chú" }
            
            // ========== HIỂN THỊ SỐ TIỀN ==========
            val amountStr = CurrencyUtils.toCurrency(transaction.amount)
            
            when (transaction.type) {
                TransactionType.INCOME, TransactionType.LOAN_TAKE -> {
                    binding.txtAmount.text = "+ $amountStr"
                    binding.txtAmount.setTextColor(Color.parseColor("#4CAF50")) // Xanh
                }
                TransactionType.EXPENSE, TransactionType.LOAN_GIVE -> {
                    binding.txtAmount.text = "- $amountStr"
                    binding.txtAmount.setTextColor(Color.parseColor("#F44336")) // Đỏ
                }
            }
            
            // ========== HIỂN THỊ NGÀY + PHƯƠNG THỨC ==========
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val paymentMethod = if (transaction.paymentMethod == "BANK") 
                "Chuyển khoản" else "Tiền mặt"
            binding.txtDate.text = "${dateFormat.format(transaction.date)} • $paymentMethod"
            
            // ========== CLICK LISTENER ==========
            itemView.setOnClickListener { onItemClick(transaction) }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<TransactionWithCategory>() {
        override fun areItemsTheSame(old: TransactionWithCategory, new: TransactionWithCategory) = 
            old.transaction.id == new.transaction.id
            
        override fun areContentsTheSame(old: TransactionWithCategory, new: TransactionWithCategory) = 
            old == new
    }
}
```

---

## 7. Tính toán thống kê

```kotlin
// Trong MainViewModel

// Tổng thu nhập tháng này
val monthlyIncome = monthlyStats.map { (income, _, _) -> income }
    .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

// Tổng chi tiêu tháng này
val monthlyExpense = monthlyStats.map { (_, expense, _) -> expense }
    .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

// Số dư tổng (tất cả thời gian)
val totalBalance = allTransactions.map { list ->
    list.sumOf { 
        when (it.transaction.type) {
            TransactionType.INCOME, TransactionType.LOAN_TAKE -> it.transaction.amount
            TransactionType.EXPENSE, TransactionType.LOAN_GIVE -> -it.transaction.amount
        }
    }
}.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)
```

---

## 8. Quick Reference

| Field | Type | Ví dụ |
|-------|------|-------|
| `id` | Long | 1, 2, 3... (auto) |
| `amount` | Double | 500000.0 |
| `categoryId` | Long | 1 (FK) |
| `paymentMethod` | String | "CASH" / "BANK" |
| `type` | TransactionType | INCOME / EXPENSE / LOAN_TAKE / LOAN_GIVE |
| `note` | String | "Ăn trưa với bạn" |
| `date` | Long | 1704067200000 (timestamp) |
| `isRecurring` | Boolean | true / false |
