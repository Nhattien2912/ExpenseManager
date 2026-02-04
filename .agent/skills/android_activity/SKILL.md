---
name: Android Activity
description: Hướng dẫn tạo và quản lý Activity trong Android với Kotlin, bao gồm lifecycle, intent, và best practices.
---

# Kỹ năng: Tạo và Quản lý Android Activity

> **Mục đích**: Skill này hướng dẫn cách tạo Activity mới, quản lý lifecycle, và truyền dữ liệu giữa các màn hình.

## Khi nào sử dụng Skill này
- Người dùng yêu cầu tạo một màn hình mới (Activity)
- Cần thiết lập navigation giữa các màn hình
- Xử lý lifecycle của Activity

---

## 1. Cấu trúc Activity chuẩn

```kotlin
// File: ui/feature/ExampleActivity.kt
// Mô tả: Activity mẫu với ViewBinding và các lifecycle methods

class ExampleActivity : AppCompatActivity() {

    // ViewBinding - thay thế findViewById
    private lateinit var binding: ActivityExampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inflate layout với ViewBinding
        binding = ActivityExampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()       // Thiết lập UI components
        setupObservers() // Observe data từ ViewModel
    }

    private fun setupUI() {
        // Thiết lập click listeners, adapters, etc.
        binding.btnSave.setOnClickListener {
            // Xử lý click
        }
    }

    private fun setupObservers() {
        // Observe LiveData/StateFlow từ ViewModel
        viewModel.data.observe(this) { data ->
            // Cập nhật UI
        }
    }

    // ========== LIFECYCLE METHODS ==========
    
    override fun onStart() {
        super.onStart()
        // Activity bắt đầu hiển thị với user
    }

    override fun onResume() {
        super.onResume()
        // Activity ở foreground, có thể tương tác
    }

    override fun onPause() {
        super.onPause()
        // Activity mất focus (dialog, app khác che)
    }

    override fun onStop() {
        super.onStop()
        // Activity không còn hiển thị
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup resources, unregister listeners
    }
}
```

---

## 2. Checklist khi tạo Activity mới

| Bước | Mô tả | File/Location |
|------|-------|---------------|
| 1 | Tạo file Kotlin | `app/src/main/java/.../ui/[feature]/[Name]Activity.kt` |
| 2 | Tạo layout XML | `app/src/main/res/layout/activity_[name].xml` |
| 3 | Đăng ký trong Manifest | `AndroidManifest.xml` |
| 4 | Tạo ViewModel (nếu cần) | `viewmodel/[Name]ViewModel.kt` |

### Đăng ký trong AndroidManifest.xml:
```xml
<!-- Thêm vào trong <application> tag -->
<activity
    android:name=".ui.feature.NameActivity"
    android:exported="false"
    android:screenOrientation="portrait" />
```

---

## 3. Truyền dữ liệu giữa các Activity

### 3.1 Gửi dữ liệu
```kotlin
// Tạo Intent và đính kèm data
val intent = Intent(this, DetailActivity::class.java).apply {
    putExtra("EXTRA_ID", itemId)        // Long
    putExtra("EXTRA_NAME", itemName)    // String
    putExtra("EXTRA_AMOUNT", amount)    // Double
}
startActivity(intent)
```

### 3.2 Nhận dữ liệu (trong Activity đích)
```kotlin
// Trong onCreate() của DetailActivity
val itemId = intent.getLongExtra("EXTRA_ID", -1L)  // -1L là default value
val itemName = intent.getStringExtra("EXTRA_NAME") ?: ""
val amount = intent.getDoubleExtra("EXTRA_AMOUNT", 0.0)
```

### 3.3 Nhận kết quả từ Activity khác
```kotlin
// Định nghĩa launcher
private val addTransactionLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == RESULT_OK) {
        // Xử lý kết quả trả về
        val newId = result.data?.getLongExtra("NEW_ID", -1L)
        // Refresh data...
    }
}

// Khởi chạy Activity
addTransactionLauncher.launch(Intent(this, AddTransactionActivity::class.java))

// Trong AddTransactionActivity - trả về kết quả
setResult(RESULT_OK, Intent().apply {
    putExtra("NEW_ID", newTransactionId)
})
finish()
```

---

## 4. Best Practices

| ✅ Nên làm | ❌ Không nên làm |
|-----------|-----------------|
| Dùng ViewBinding | Dùng findViewById |
| Logic trong ViewModel | Logic trong Activity |
| Định nghĩa EXTRA keys là constants | Hardcode string keys |
| Xử lý configuration change | Bỏ qua xoay màn hình |

### Ví dụ định nghĩa constants:
```kotlin
companion object {
    // Constants cho Intent extras
    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    const val EXTRA_IS_EDIT_MODE = "extra_is_edit_mode"
    
    // Factory method để tạo Intent chuẩn
    fun newIntent(context: Context, transactionId: Long, isEditMode: Boolean = false): Intent {
        return Intent(context, DetailActivity::class.java).apply {
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            putExtra(EXTRA_IS_EDIT_MODE, isEditMode)
        }
    }
}

// Sử dụng:
startActivity(DetailActivity.newIntent(this, transactionId = 123L, isEditMode = true))
```

---

## 5. Áp dụng trong ExpenseManager

Các Activity hiện có trong dự án:
- `MainActivity` - Màn hình chính với Bottom Navigation
- `AddTransactionActivity` - Thêm/sửa giao dịch
- `SettingActivity` - Cài đặt ứng dụng

Khi tạo Activity mới, follow pattern của `AddTransactionActivity` với:
- ViewBinding
- ViewModel + StateFlow
- Intent extras với companion object constants
