---
name: Android Room Database
description: Hướng dẫn sử dụng Room Database trong Android để lưu trữ dữ liệu local.
---

# Kỹ năng: Room Database trong Android

## Khi nào sử dụng
- Cần lưu trữ dữ liệu offline/local
- Làm việc với SQLite một cách type-safe
- Cần reactive database với Flow/LiveData

## Các thành phần chính

### 1. Entity (Bảng dữ liệu)

```kotlin
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "amount")
    val amount: Double,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    
    @ColumnInfo(name = "date")
    val date: Long,  // Timestamp
    
    @ColumnInfo(name = "is_expense")
    val isExpense: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2. DAO (Data Access Object)

```kotlin
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE is_expense = 0")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE is_expense = 1")
    fun getTotalExpense(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
```

### 3. Database

```kotlin
@Database(
    entities = [Transaction::class, Category::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_manager_db"
                )
                .fallbackToDestructiveMigration()  // Chỉ dùng khi dev
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### 4. Type Converters (cho các kiểu phức tạp)

```kotlin
class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, type)
    }
}
```

## Repository Pattern

```kotlin
class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    val totalIncome: Flow<Double> = transactionDao.getTotalIncome()
        .map { it ?: 0.0 }
    
    val totalExpense: Flow<Double> = transactionDao.getTotalExpense()
        .map { it ?: 0.0 }

    suspend fun insert(transaction: Transaction): Long {
        return transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun getById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }
}
```

## Database Migration

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE transactions ADD COLUMN notes TEXT DEFAULT ''"
        )
    }
}

// Trong Database builder
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
    .build()
```

## Relations (Quan hệ giữa các bảng)

```kotlin
// One-to-Many: Category có nhiều Transaction
data class CategoryWithTransactions(
    @Embedded val category: Category,
    @Relation(
        parentColumn = "id",
        entityColumn = "category_id"
    )
    val transactions: List<Transaction>
)

// Trong DAO
@Transaction
@Query("SELECT * FROM categories WHERE id = :categoryId")
suspend fun getCategoryWithTransactions(categoryId: Long): CategoryWithTransactions
```

## Dependencies

```groovy
// build.gradle (app)
def room_version = "2.6.1"

dependencies {
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-ktx:$room_version"  // Coroutines support
    kapt "androidx.room:room-compiler:$room_version"
    
    // Optional - Test helpers
    testImplementation "androidx.room:room-testing:$room_version"
}

// Nếu dùng KSP thay vì KAPT
// ksp "androidx.room:room-compiler:$room_version"
```

## Best Practices

1. **Luôn sử dụng suspend functions** hoặc Flow cho database operations
2. **Không chạy database operations trên Main Thread**
3. **Sử dụng Repository pattern** để abstract data source
4. **Export schema** để track database changes
5. **Viết proper migrations** thay vì dùng destructive migration trong production
6. **Sử dụng indices** cho các column thường query:
   ```kotlin
   @Entity(
       tableName = "transactions",
       indices = [Index(value = ["date"]), Index(value = ["category_id"])]
   )
   ```
