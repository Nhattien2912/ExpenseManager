# 💰 ExpenseManager

Ứng dụng Android quản lý chi tiêu cá nhân được xây dựng với Kotlin và kiến trúc MVVM.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-SDK%2035-green.svg)](https://developer.android.com/)
[![Room](https://img.shields.io/badge/Room-2.6.1-blue.svg)](https://developer.android.com/training/data-storage/room)

---

## 📱 Screenshots

<!-- Thêm screenshots sau -->

---

## ✨ Tính năng

### Đã hoàn thiện ✅
- **Quản lý giao dịch**: Thêm, sửa, xóa thu/chi
- **Danh mục**: Quản lý categories với icons emoji
- **Bộ lọc**: Lọc theo ngày/tháng, loại giao dịch
- **Calendar View**: Xem tổng thu/chi theo ngày trong tháng
- **Biểu đồ**: Pie, Bar, Line charts với MPAndroidChart
- **Backup/Restore**: Export/Import JSON
- **Swipe-to-delete**: Vuốt để xóa giao dịch
- **Recurring transactions**: Giao dịch lặp lại
- **Quản lý nợ**: Theo dõi cho vay/đi vay
- **Ngân sách**: Đặt hạn mức chi tiêu tháng
- **Tutorial**: Hướng dẫn người dùng mới với TapTargetView
- **Đa tiền tệ**: Hỗ trợ VND và USD

### Đang phát triển 🚧
- Google Drive sync
- Firebase cloud backup
- Dark mode
- Widget màn hình chính

---

## 🏗️ Kiến trúc

```
com.nhattien.expensemanager/
├── data/                    # Data Layer
│   ├── entity/              # Room entities
│   ├── dao/                 # Data Access Objects
│   ├── database/            # AppDatabase
│   └── repository/          # Repository pattern
│
├── domain/                  # Domain Layer
│   ├── TransactionType.kt   # INCOME, EXPENSE, LOAN
│   ├── FilterType.kt        # Filter enums
│   └── ChartType.kt         # Chart types
│
├── ui/                      # UI Layer
│   ├── main/                # MainActivity, Fragments
│   ├── add/                 # Add Transaction
│   └── adapter/             # RecyclerView Adapters
│
├── viewmodel/               # ViewModel Layer
│   └── MainViewModel.kt     # MVVM with StateFlow
│
└── utils/                   # Utilities
    ├── CurrencyUtils.kt     # Format VND/USD
    ├── DateUtils.kt         # Date helpers
    └── BackupUtils.kt       # JSON export/import
```

---

## 🛠️ Công nghệ sử dụng

| Công nghệ | Version | Mục đích |
|-----------|---------|----------|
| **Kotlin** | 1.9.22 | Ngôn ngữ chính |
| **Android SDK** | 35 | Target/Compile SDK |
| **Room** | 2.6.1 | Local database |
| **Lifecycle** | 2.8.4 | ViewModel, LiveData |
| **Coroutines** | Built-in | Async operations |
| **StateFlow** | Built-in | Reactive UI state |
| **Material 3** | 1.12.0 | UI components |
| **MPAndroidChart** | 3.1.0 | Charts |
| **TapTargetView** | 1.13.3 | Tutorial spotlights |
| **Gson** | 2.10.1 | JSON parsing |
| **Firebase** | 33.1.0 | Auth, Database |
| **Google Drive API** | 2.2.0 | Cloud backup |

---

## � Cài đặt

### Yêu cầu
- Android Studio Hedgehog (2023.1.1) trở lên
- JDK 17
- Android SDK 35
- Min SDK 24 (Android 7.0)

### Build

```bash
# Clone repo
git clone https://github.com/Nhattien2912/ExpenseManager.git

# Mở bằng Android Studio
# Sync Gradle
# Run app
```

---

## 🤖 Agent Skills

Dự án có **31 Agent Skills** để hỗ trợ AI coding assistant:

```
.agent/skills/
├── 00_skill_guide/          # Hướng dẫn sử dụng skills
├── 00_dependencies/         # Versions & dependencies
│
├── kotlin_oop/              # OOP trong Kotlin
├── mvvm_pattern/            # MVVM architecture
├── project_structure/       # Cấu trúc thư mục
│
├── android_activity/        # Activity lifecycle
├── android_fragment/        # Fragment
├── android_viewmodel/       # ViewModel, StateFlow
├── android_room_database/   # Room Database
├── android_recyclerview/    # RecyclerView, Adapters
├── android_navigation/      # Navigation Component
├── kotlin_coroutines/       # Coroutines, Flow
├── android_debugging/       # Debug, crash logs
│
├── expense_architecture/    # Kiến trúc dự án
├── expense_transactions/    # Transaction entity
├── expense_categories/      # Category management
├── expense_calendar/        # Calendar utilities
├── expense_currency/        # Currency formatting
├── expense_charts/          # MPAndroidChart
├── expense_backup/          # Backup/Restore
├── expense_tutorial/        # TapTargetView
│
├── jetpack_compose/         # Modern UI (future)
├── hilt_di/                 # Dependency Injection
├── datastore/               # Replace SharedPrefs
├── workmanager/             # Background tasks
├── material_design_3/       # Material You
├── unit_testing/            # JUnit, MockK
├── retrofit_networking/     # HTTP client
├── biometric_auth/          # Fingerprint/Face
├── app_widgets/             # Home widgets
└── notifications/           # Notifications
```

---

## 📄 License

MIT License - Xem file [LICENSE](LICENSE) để biết thêm chi tiết.

---

## 👤 Tác giả

**Nhattien2912**

- GitHub: [@Nhattien2912](https://github.com/Nhattien2912)

---

## 🤝 Đóng góp

Mọi đóng góp đều được hoan nghênh! Hãy tạo Pull Request hoặc Issue.

1. Fork dự án
2. Tạo branch (`git checkout -b feature/TinhNangMoi`)
3. Commit (`git commit -m 'Thêm tính năng mới'`)
4. Push (`git push origin feature/TinhNangMoi`)
5. Tạo Pull Request
