---
name: Skill Usage Guide
description: Hướng dẫn AI cách sử dụng các skills đúng cách. ĐÂY LÀ SKILL BẮT BUỘC ĐỌC ĐẦU TIÊN.
---

# Hướng dẫn Sử dụng Skills

> **⚠️ AI PHẢI ĐỌC SKILL NÀY TRƯỚC KHI ÁP DỤNG BẤT KỲ SKILL NÀO KHÁC**

---

## 1. Quy trình Áp dụng Skill

```
┌─────────────────────────────────────────────────────────────────┐
│  BƯỚC 1: User yêu cầu tính năng                                 │
│  Ví dụ: "Thêm màn hình chi tiết giao dịch"                      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  BƯỚC 2: Xác định skills liên quan                              │
│  → android_activity (tạo Activity mới)                          │
│  → expense_transactions (làm việc với Transaction)              │
│  → expense_architecture (follow MVVM pattern)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  BƯỚC 3: Kiểm tra dependencies                                  │
│  → Đọc 00_dependencies/SKILL.md                                 │
│  → Xác nhận tất cả dependencies đã có                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  BƯỚC 4: Đọc và áp dụng từng skill                              │
│  → Follow code patterns trong skill                             │
│  → Sử dụng đúng naming conventions                              │
│  → Import đúng packages                                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  BƯỚC 5: Implement                                              │
│  → Tạo files theo project_structure                             │
│  → Viết code theo patterns                                      │
│  → Test và verify                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Mapping: Yêu cầu → Skills

### Tạo UI mới
| Yêu cầu | Skills cần đọc |
|---------|----------------|
| Tạo Activity mới | `android_activity`, `project_structure` |
| Tạo Fragment mới | `android_fragment`, `project_structure` |
| Tạo RecyclerView | `android_recyclerview`, `expense_transactions` (xem adapter mẫu) |
| Navigation giữa màn hình | `android_navigation` |

### Làm việc với Data
| Yêu cầu | Skills cần đọc |
|---------|----------------|
| Tạo Entity mới | `android_room_database`, `expense_transactions` |
| Query database | `android_room_database`, `kotlin_coroutines` |
| Tạo ViewModel | `android_viewmodel`, `expense_architecture` |
| Format tiền | `expense_currency` |
| Xử lý ngày tháng | `expense_calendar` |

### Tính năng đặc thù ExpenseManager
| Yêu cầu | Skills cần đọc |
|---------|----------------|
| Thêm/sửa Transaction | `expense_transactions`, `expense_categories` |
| Hiển thị biểu đồ | `expense_charts` |
| Backup/Restore | `expense_backup` |
| Tutorial cho user | `expense_tutorial` |
| Làm việc với Calendar | `expense_calendar` |

### Công nghệ nâng cao (Cần thêm dependencies)
| Yêu cầu | Skills cần đọc | Dependencies |
|---------|----------------|--------------|
| Chuyển sang Compose | `jetpack_compose` | ⚠️ Cần thêm |
| Dependency Injection | `hilt_di` | ⚠️ Cần thêm |
| Thay SharedPreferences | `datastore` | ⚠️ Cần thêm |
| Background tasks | `workmanager` | ⚠️ Cần thêm |
| API calls | `retrofit_networking` | ⚠️ Cần thêm |
| Vân tay/Face ID | `biometric_auth` | ⚠️ Cần thêm |
| Widget màn hình chính | `app_widgets` | Có sẵn |
| Thông báo | `notifications` | Có sẵn |

---

## 3. Checklist trước khi Implement

### ✅ Đã đọc skills liên quan?
- [ ] Skill chính cho tính năng
- [ ] `expense_architecture` (MVVM pattern)
- [ ] `project_structure` (naming, organization)
- [ ] `00_dependencies` (versions)

### ✅ Đã kiểm tra dependencies?
- [ ] Tất cả imports có sẵn trong project
- [ ] Không cần thêm library mới
- [ ] Hoặc đã thông báo user về dependencies cần thêm

### ✅ Đã follow conventions?
- [ ] Đặt file đúng thư mục
- [ ] Naming theo pattern: `[Name]Activity.kt`, `[Name]Fragment.kt`...
- [ ] Layout: `activity_[name].xml`, `fragment_[name].xml`...
- [ ] ViewBinding enabled

### ✅ Đã follow MVVM?
- [ ] Logic trong ViewModel, không trong Activity/Fragment
- [ ] Data expose qua StateFlow/LiveData
- [ ] Repository cho database operations

---

## 4. Ví dụ Áp dụng

### Ví dụ: User yêu cầu "Thêm màn hình chi tiết giao dịch"

**Bước 1: Xác định skills**
```
- android_activity (tạo DetailTransactionActivity)
- expense_transactions (hiểu Transaction entity)
- expense_architecture (follow MVVM)
- expense_currency (format tiền)
- project_structure (đặt file đúng chỗ)
```

**Bước 2: Kiểm tra dependencies**
```
Đọc 00_dependencies → Tất cả đã có ✅
```

**Bước 3: Tạo files**
```
ui/detail/DetailTransactionActivity.kt
res/layout/activity_detail_transaction.xml
viewmodel/DetailTransactionViewModel.kt
```

**Bước 4: Implement theo patterns từ skills**
```kotlin
// Follow android_activity pattern
class DetailTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailTransactionBinding
    private val viewModel: DetailTransactionViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // ...follow skill pattern
    }
}
```

---

## 5. Khi Skill và Project Code Khác nhau

> **Ưu tiên: PROJECT CODE > SKILL**

Nếu phát hiện code trong project khác với skill:
1. **Skill là hướng dẫn tổng quát**
2. **Project code là implementation thực tế**
3. **Follow project code** vì đó là cách team đã chọn
4. **Cập nhật skill** nếu cần thiết

---

## 6. Cập nhật Skills

Khi cần cập nhật skill (versions mới, patterns mới):

```markdown
---
name: Tên Skill
description: Mô tả
updated: 2026-02-03
---
```

Thêm changelog ở cuối skill:
```markdown
## Changelog
- 2026-02-03: Cập nhật Room lên 2.6.1
- 2026-01-15: Thêm ví dụ StateFlow
```
