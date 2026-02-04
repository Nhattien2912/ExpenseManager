---
name: ExpenseManager Categories
description: Quản lý Categories trong ExpenseManager - tạo, sửa, xóa danh mục.
---

# Categories trong ExpenseManager

## CategoryEntity

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,  // Emoji hoặc icon code
    val color: String = "#FF5722",  // Hex color
    val isIncome: Boolean = false   // true = income category
)
```

## Default Categories

```kotlin
val defaultCategories = listOf(
    // Chi tiêu
    CategoryEntity(name = "Ăn uống", icon = "🍔", isIncome = false),
    CategoryEntity(name = "Di chuyển", icon = "🚗", isIncome = false),
    CategoryEntity(name = "Mua sắm", icon = "🛒", isIncome = false),
    CategoryEntity(name = "Hóa đơn", icon = "📄", isIncome = false),
    CategoryEntity(name = "Giải trí", icon = "🎮", isIncome = false),
    CategoryEntity(name = "Sức khỏe", icon = "💊", isIncome = false),
    
    // Thu nhập
    CategoryEntity(name = "Lương", icon = "💰", isIncome = true),
    CategoryEntity(name = "Thưởng", icon = "🎁", isIncome = true),
    CategoryEntity(name = "Đầu tư", icon = "📈", isIncome = true)
)
```

## CategoryRepository

```kotlin
class CategoryRepository(private val dao: CategoryDao) {
    
    suspend fun getAllCategories(): List<CategoryEntity> {
        return dao.getAll()
    }
    
    suspend fun getExpenseCategories(): List<CategoryEntity> {
        return dao.getByType(isIncome = false)
    }
    
    suspend fun getIncomeCategories(): List<CategoryEntity> {
        return dao.getByType(isIncome = true)
    }
    
    suspend fun insert(category: CategoryEntity) {
        dao.insert(category)
    }
    
    suspend fun delete(category: CategoryEntity) {
        dao.delete(category)
    }
}
```

## CategoryDao

```kotlin
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE isIncome = :isIncome")
    suspend fun getByType(isIncome: Boolean): List<CategoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)
    
    @Delete
    suspend fun delete(category: CategoryEntity)
}
```

## Hiển thị Category trong Adapter

```kotlin
class CategoryAdapter(
    private val onCategoryClick: (CategoryEntity) -> Unit
) : ListAdapter<CategoryEntity, ...>(...) {
    
    fun bind(category: CategoryEntity) {
        txtIcon.text = category.icon
        txtName.text = category.name
        
        // Tạo background với màu category
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(category.color))
        }
        iconBackground.background = drawable
        
        itemView.setOnClickListener { onCategoryClick(category) }
    }
}
```

## Chọn Category khi thêm Transaction

```kotlin
// Trong AddTransactionActivity
private var selectedCategory: CategoryEntity? = null

private fun setupCategorySelector() {
    viewModel.categories.observe(this) { categories ->
        val filtered = if (isExpense) {
            categories.filter { !it.isIncome }
        } else {
            categories.filter { it.isIncome }
        }
        categoryAdapter.submitList(filtered)
    }
    
    categoryAdapter = CategoryAdapter { category ->
        selectedCategory = category
        binding.txtSelectedCategory.text = "${category.icon} ${category.name}"
    }
}
```
