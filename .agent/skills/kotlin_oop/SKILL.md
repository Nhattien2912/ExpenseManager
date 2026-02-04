---
name: Kotlin OOP Fundamentals
description: Các khái niệm lập trình hướng đối tượng (OOP) trong Kotlin - Class, Inheritance, Interface, Data Class.
---

# Kotlin OOP Fundamentals

> **Mục đích**: Skill này hướng dẫn các khái niệm OOP cơ bản và nâng cao trong Kotlin, áp dụng cho Android development.

---

## 1. Class và Object

### 1.1 Khai báo Class cơ bản

```kotlin
// ========== CLASS ĐƠN GIẢN ==========
class Transaction {
    var amount: Double = 0.0
    var note: String = ""
    
    fun display() {
        println("Amount: $amount, Note: $note")
    }
}

// Sử dụng:
val transaction = Transaction()
transaction.amount = 500000.0
```

### 1.2 Primary Constructor

```kotlin
// Constructor trong khai báo class
class Transaction(
    val id: Long,              // val = read-only (getter only)
    var amount: Double,        // var = read-write (getter + setter)
    val note: String = ""      // Default value
)

// Sử dụng:
val t1 = Transaction(1, 500000.0, "Ăn trưa")
val t2 = Transaction(2, 100000.0)  // note = "" (default)
```

### 1.3 Init Block và Secondary Constructor

```kotlin
class Transaction(
    val id: Long,
    var amount: Double
) {
    var formattedAmount: String = ""
    
    // Init block - chạy sau primary constructor
    init {
        require(amount >= 0) { "Amount phải >= 0" }
        formattedAmount = CurrencyUtils.toCurrency(amount)
    }
    
    // Secondary constructor
    constructor(amount: Double) : this(0, amount)
}
```

---

## 2. Encapsulation (Đóng gói)

```kotlin
class BankAccount(
    val accountNumber: String
) {
    // ========== PRIVATE: chỉ truy cập trong class ==========
    private var _balance: Double = 0.0
    
    // ========== PUBLIC GETTER: expose ra ngoài (read-only) ==========
    val balance: Double
        get() = _balance
    
    // ========== INTERNAL: truy cập trong cùng module ==========
    internal var bankCode: String = ""
    
    // ========== PROTECTED: truy cập trong class và subclass ==========
    protected var interestRate: Double = 0.05
    
    // ========== PUBLIC METHODS ==========
    fun deposit(amount: Double) {
        require(amount > 0) { "Số tiền phải > 0" }
        _balance += amount
    }
    
    fun withdraw(amount: Double): Boolean {
        if (amount > _balance) return false
        _balance -= amount
        return true
    }
}

// Sử dụng:
val account = BankAccount("001")
account.deposit(1000000.0)
println(account.balance)        // OK: đọc được
// account._balance = 0         // ERROR: private, không truy cập được
```

### Visibility Modifiers

| Modifier | Trong Class | Subclass | Cùng Module | Ngoài Module |
|----------|-------------|----------|-------------|--------------|
| `private` | ✅ | ❌ | ❌ | ❌ |
| `protected` | ✅ | ✅ | ❌ | ❌ |
| `internal` | ✅ | ✅ | ✅ | ❌ |
| `public` (default) | ✅ | ✅ | ✅ | ✅ |

---

## 3. Inheritance (Kế thừa)

```kotlin
// ========== OPEN CLASS: cho phép kế thừa ==========
open class BaseTransaction(
    open val id: Long,
    open var amount: Double
) {
    // Open function: cho phép override
    open fun getDisplayAmount(): String {
        return amount.toString()
    }
    
    // Final function (default): không cho override
    fun getType(): String = "BASE"
}

// ========== SUBCLASS ==========
class ExpenseTransaction(
    override val id: Long,
    override var amount: Double,
    val category: String
) : BaseTransaction(id, amount) {
    
    // Override method
    override fun getDisplayAmount(): String {
        return "- ${CurrencyUtils.toCurrency(amount)}"
    }
    
    // Thêm method riêng
    fun getCategoryIcon(): String {
        return when (category) {
            "food" -> "🍔"
            "transport" -> "🚗"
            else -> "💰"
        }
    }
}

class IncomeTransaction(
    override val id: Long,
    override var amount: Double,
    val source: String
) : BaseTransaction(id, amount) {
    
    override fun getDisplayAmount(): String {
        return "+ ${CurrencyUtils.toCurrency(amount)}"
    }
}
```

---

## 4. Abstract Class

```kotlin
// ========== ABSTRACT CLASS: không thể tạo instance trực tiếp ==========
abstract class BaseViewModel : ViewModel() {
    
    // Abstract property: phải implement trong subclass
    abstract val screenTitle: String
    
    // Abstract method: phải implement trong subclass
    abstract fun loadData()
    
    // Concrete method: có thể sử dụng ngay
    fun showLoading() {
        // Logic chung cho tất cả ViewModels
    }
    
    protected fun handleError(error: Throwable) {
        Log.e("ViewModel", "Error: ${error.message}")
    }
}

// ========== CONCRETE CLASS ==========
class MainViewModel : BaseViewModel() {
    
    override val screenTitle = "Tổng quan"
    
    override fun loadData() {
        viewModelScope.launch {
            showLoading()
            // Load transactions...
        }
    }
}
```

---

## 5. Interface

```kotlin
// ========== INTERFACE: định nghĩa contract ==========
interface TransactionRepository {
    // Abstract method
    suspend fun getAll(): List<Transaction>
    suspend fun insert(transaction: Transaction): Long
    suspend fun delete(transaction: Transaction)
    
    // Default implementation
    suspend fun getById(id: Long): Transaction? {
        return getAll().find { it.id == id }
    }
}

// ========== IMPLEMENT INTERFACE ==========
class LocalTransactionRepository(
    private val dao: TransactionDao
) : TransactionRepository {
    
    override suspend fun getAll(): List<Transaction> {
        return dao.getAll()
    }
    
    override suspend fun insert(transaction: Transaction): Long {
        return dao.insert(transaction)
    }
    
    override suspend fun delete(transaction: Transaction) {
        dao.delete(transaction)
    }
    
    // getById() đã có default implementation, không cần override
}

// ========== MULTIPLE INTERFACES ==========
interface Exportable {
    fun toJson(): String
    fun toCsv(): String
}

interface Comparable<T> {
    fun compareTo(other: T): Int
}

class Transaction : Exportable, Comparable<Transaction> {
    override fun toJson(): String { ... }
    override fun toCsv(): String { ... }
    override fun compareTo(other: Transaction): Int { ... }
}
```

---

## 6. Data Class

```kotlin
// ========== DATA CLASS: tự động generate equals, hashCode, toString, copy ==========
data class TransactionEntity(
    val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)

// Tự động có các methods:
val t1 = TransactionEntity(1, 500000.0, 1)
val t2 = TransactionEntity(1, 500000.0, 1)

println(t1 == t2)           // true (equals so sánh tất cả properties)
println(t1.hashCode())      // hashCode để dùng trong HashMap, HashSet
println(t1.toString())      // "TransactionEntity(id=1, amount=500000.0, ...)"

// ========== COPY: tạo bản sao với một số field thay đổi ==========
val t3 = t1.copy(amount = 600000.0)  // Chỉ đổi amount, giữ nguyên các field khác

// ========== DESTRUCTURING ==========
val (id, amount, catId, note, date) = t1
println("ID: $id, Amount: $amount")
```

---

## 7. Sealed Class

```kotlin
// ========== SEALED CLASS: giới hạn các subclass ==========
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Sử dụng với when (compiler biết tất cả cases)
fun handleResult(result: Result<List<Transaction>>) {
    when (result) {
        is Result.Success -> showTransactions(result.data)
        is Result.Error -> showError(result.message)
        is Result.Loading -> showLoading()
        // Không cần else vì đã cover hết
    }
}

// ========== SEALED CLASS cho UI State ==========
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val transactions: List<Transaction>) : UiState()
    data class Error(val message: String) : UiState()
}
```

---

## 8. Object và Companion Object

```kotlin
// ========== SINGLETON với object ==========
object CurrencyUtils {
    var checkCurrency: Int = 0
    
    fun toCurrency(amount: Double): String {
        // ...
    }
}
// Sử dụng: CurrencyUtils.toCurrency(100.0)

// ========== COMPANION OBJECT: static-like members ==========
class Transaction(val id: Long, val amount: Double) {
    
    companion object {
        // Constants
        const val TYPE_INCOME = 0
        const val TYPE_EXPENSE = 1
        
        // Factory method
        fun createExpense(amount: Double): Transaction {
            return Transaction(0, -amount)
        }
        
        // Utility
        fun fromJson(json: String): Transaction {
            // Parse JSON...
        }
    }
}

// Sử dụng:
val type = Transaction.TYPE_INCOME
val expense = Transaction.createExpense(500000.0)
```

---

## 9. Extension Functions

```kotlin
// ========== THÊM FUNCTION VÀO CLASS CÓ SẴN ==========
fun Double.toCurrency(): String {
    return CurrencyUtils.toCurrency(this)
}

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

// Sử dụng:
val amount = 500000.0
println(amount.toCurrency())  // "500.000 đ"

val timestamp = System.currentTimeMillis()
println(timestamp.toFormattedDate())  // "03/02/2024"

// ========== EXTENSION PROPERTY ==========
val Transaction.isExpense: Boolean
    get() = this.type == TransactionType.EXPENSE
```

---

## 10. Quick Reference

| Concept | Keyword | Mô tả |
|---------|---------|-------|
| Class thường | `class` | Không thể kế thừa (final mặc định) |
| Class mở | `open class` | Cho phép kế thừa |
| Abstract | `abstract class` | Không tạo instance trực tiếp |
| Data class | `data class` | Auto equals, hashCode, copy |
| Sealed class | `sealed class` | Giới hạn subclasses |
| Interface | `interface` | Contract, multiple inheritance |
| Object | `object` | Singleton |
| Companion | `companion object` | Static members |
