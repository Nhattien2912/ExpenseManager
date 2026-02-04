---
name: ExpenseManager Dependencies
description: Danh sách dependencies và versions hiện tại của dự án ExpenseManager. AI PHẢI đọc skill này trước khi thêm code mới.
---

# ExpenseManager Dependencies & Versions

> **⚠️ QUAN TRỌNG**: Skill này chứa thông tin về versions và dependencies hiện tại của dự án. AI phải sử dụng đúng versions này khi thêm code mới.

---

## 1. Versions hiện tại (Cập nhật: 2026-02)

### Core Android
| Library | Version | Ghi chú |
|---------|---------|---------|
| Kotlin | `1.9.22` | Ngôn ngữ chính |
| Android Gradle Plugin | `8.13.2` | Build tool |
| Min SDK | `24` | Android 7.0+ |
| Target SDK | `35` | Android 15 |
| Compile SDK | `35` | |

### AndroidX
| Library | Version | Import |
|---------|---------|--------|
| Core KTX | `1.13.1` | `androidx.core:core-ktx` |
| AppCompat | `1.7.0` | `androidx.appcompat:appcompat` |
| Activity | `1.9.3` | `androidx.activity:activity` |
| ConstraintLayout | `2.2.1` | `androidx.constraintlayout:constraintlayout` |
| Material | `1.12.0` | `com.google.android.material:material` |

### Lifecycle & ViewModel
| Library | Version | Import |
|---------|---------|--------|
| Lifecycle ViewModel KTX | `2.8.4` | `androidx.lifecycle:lifecycle-viewmodel-ktx` |
| Lifecycle Runtime KTX | `2.8.4` | `androidx.lifecycle:lifecycle-runtime-ktx` |
| Fragment KTX | `1.8.2` | `androidx.fragment:fragment-ktx` |

### Room Database
| Library | Version | Import |
|---------|---------|--------|
| Room Runtime | `2.6.1` | `androidx.room:room-runtime` |
| Room KTX | `2.6.1` | `androidx.room:room-ktx` |
| Room Compiler | `2.6.1` | `kapt "androidx.room:room-compiler"` |

### Third-party
| Library | Version | Mục đích |
|---------|---------|----------|
| MPAndroidChart | `v3.1.0` | Biểu đồ |
| Gson | `2.10.1` | JSON parsing |
| TapTargetView | `1.13.3` | Tutorial spotlight |

### Firebase & Google
| Library | Version |
|---------|---------|
| Firebase BOM | `33.1.0` |
| Play Services Auth | `21.3.0` |
| Google API Client | `2.2.0` |

---

## 2. Cách thêm Dependency mới

### 2.1 Sử dụng Version Catalog (Recommended)

```toml
# Thêm vào gradle/libs.versions.toml

[versions]
newLibrary = "1.0.0"

[libraries]
new-library = { group = "com.example", name = "library", version.ref = "newLibrary" }
```

```kotlin
// Trong app/build.gradle.kts
dependencies {
    implementation(libs.new.library)
}
```

### 2.2 Direct Declaration

```kotlin
// Trong app/build.gradle.kts
dependencies {
    implementation("com.example:library:1.0.0")
}
```

---

## 3. Hướng dẫn cho AI

### 3.1 Khi thêm code mới

1. **KIỂM TRA** dependency đã tồn tại trong `libs.versions.toml` hoặc `build.gradle.kts`
2. **SỬ DỤNG** đúng version đã có trong dự án
3. **KHÔNG** tự ý thêm dependency mới mà không thông báo user
4. **FOLLOW** patterns đã có trong codebase

### 3.2 Mapping Skill → Dependency

| Khi áp dụng Skill | Dependency cần có |
|-------------------|-------------------|
| `android_room_database` | Room (đã có ✅) |
| `android_viewmodel` | Lifecycle (đã có ✅) |
| `kotlin_coroutines` | Lifecycle (đã có ✅) |
| `expense_charts` | MPAndroidChart (đã có ✅) |
| `expense_backup` | Gson (đã có ✅) |
| `expense_tutorial` | TapTargetView (đã có ✅) |
| `jetpack_compose` | ❌ Chưa có - cần thêm |
| `hilt_di` | ❌ Chưa có - cần thêm |
| `datastore` | ❌ Chưa có - cần thêm |
| `workmanager` | ❌ Chưa có - cần thêm |
| `retrofit_networking` | ❌ Chưa có - cần thêm |
| `biometric_auth` | ❌ Chưa có - cần thêm |

### 3.3 Khi user yêu cầu tính năng mới

```
1. Đọc skill tương ứng
2. Kiểm tra dependency trong skill này
3. Nếu dependency chưa có → Thông báo user và hỏi có muốn thêm không
4. Nếu đã có → Tiến hành implement theo skill
```

---

## 4. Import Statements Reference

### Room Database
```kotlin
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Database
import androidx.room.Room
import androidx.room.OnConflictStrategy
import androidx.room.Relation
import androidx.room.Embedded
import androidx.room.TypeConverter
import androidx.room.TypeConverters
```

### ViewModel & Lifecycle
```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.activity.viewModels
```

### Coroutines & Flow
```kotlin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
```

### ViewBinding
```kotlin
// Activity
private lateinit var binding: ActivityMainBinding
binding = ActivityMainBinding.inflate(layoutInflater)
setContentView(binding.root)

// Fragment
private var _binding: FragmentHomeBinding? = null
private val binding get() = _binding!!
_binding = FragmentHomeBinding.inflate(inflater, container, false)
```

---

## 5. Sync Status

| Skill | Synced với Project | Ghi chú |
|-------|-------------------|---------|
| `expense_architecture` | ✅ | Đúng với cấu trúc hiện tại |
| `expense_transactions` | ✅ | Đúng với TransactionEntity |
| `expense_currency` | ✅ | Đúng với CurrencyUtils |
| `android_room_database` | ✅ | Room 2.6.1 |
| `android_viewmodel` | ✅ | Lifecycle 2.8.4 |
| `kotlin_coroutines` | ✅ | Có sẵn với Lifecycle |
| `jetpack_compose` | ⚠️ | Chưa có trong project |
| `hilt_di` | ⚠️ | Chưa có trong project |

> **Ghi chú**: Các skill đánh dấu ⚠️ là công nghệ mới, cần thêm dependencies trước khi sử dụng.
