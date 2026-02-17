# 🗺️ ExpenseManager Roadmap

> Danh sách tính năng cần nâng cấp và phát triển cho ExpenseManager.  
> Cập nhật: 17/02/2026

---

## 📊 Trạng thái hiện tại

### ✅ Đã hoàn thiện (Cơ bản)

| # | Tính năng | Mô tả |
|---|-----------|-------|
| 1 | Quản lý thu/chi | Thêm, sửa, xóa giao dịch |
| 2 | Categories | Danh mục với icons emoji |
| 3 | Bộ lọc | Theo ngày/tháng, loại giao dịch |
| 4 | Calendar View | Tổng thu/chi theo ngày |
| 5 | Biểu đồ | Pie, Bar, Line charts |
| 6 | Swipe-to-delete | Vuốt xóa giao dịch |
| 7 | Recurring | Giao dịch lặp lại (đánh dấu) |
| 8 | Quản lý nợ | Cho vay, đi vay, trả nợ |
| 9 | Tiết kiệm | Gửi/Rút tiết kiệm |
| 10 | Ngân sách | Spending limit tháng + theo danh mục |
| 11 | Backup JSON | Export/Import local |
| 12 | CSV Export | Xuất file CSV |
| 13 | Tutorial | TapTargetView hướng dẫn |
| 14 | Đa tiền tệ | VND / USD |

### ✅ Đã hoàn thiện (Roadmap Features)

| # | Tính năng | Phase | Bằng chứng trong code |
|---|-----------|-------|-----------------------|
| 15 | 🌙 Dark Mode | 1.1 | `themes.xml` (night), toggle trong Settings |
| 16 | 🔔 Notifications & Reminders | 1.2 | `NotificationHelper.kt`, `ReminderReceiver.kt`, `BootReceiver.kt` |
| 17 | 📱 Home Screen Widget | 1.4 | `ExpenseWidgetProvider.kt`, widget layouts |
| 18 | 🏷️ Tags / Labels | 2.3 | `TagEntity`, `TagDao`, `TransactionTagCrossRef`, `ManageTagsActivity`, filter |

### ✅ Tính năng bonus (Không có trong Roadmap gốc)

| # | Tính năng | Bằng chứng |
|---|-----------|------------|
| 19 | � Multi-Wallet | `WalletEntity`, `WalletDao`, `ManageWalletsActivity`, `WalletAdapter` |
| 20 | 📅 Chi tiêu dự kiến | `PlannedExpenseEntity`, `PlannedExpenseDao`, `PlannedExpenseActivity` |
| 21 | 🎛️ Trang Dịch vụ | `ServicesFragment.kt` với 9-item grid |
| 22 | 📊 Premium Charts | Pie/Bar/Line charts nâng cấp (outside labels, gradient bars, bezier lines) |
| 23 | 🔔 Notification Center | `NotificationEntity`, `NotificationDao`, lưu lịch sử thông báo |

---

## �🚀 Phase 1: Cải thiện UX (Ưu tiên cao)

### 1.1 🌙 Dark Mode ✅ HOÀN THÀNH
- [x] Tạo theme dark trong `themes.xml`
- [x] Toggle trong Settings
- [x] Lưu preference với SharedPreferences
- [x] Áp dụng cho toàn app

**Độ khó:** ⭐⭐  
**Thời gian thực tế:** ~30 phút

---

### 1.2 🔔 Notifications & Reminders ✅ HOÀN THÀNH
- [x] Nhắc nhở ghi chép hàng ngày (configurable time) → `scheduleDailyReminder(hour, minute)`
- [x] Cảnh báo khi chi tiêu vượt 80%, 100% ngân sách → `showBudgetWarning(percentage)`
- [x] Nhắc nợ đến hạn → `showDebtReminder(debtId, name, amount)`
- [x] Notification channels riêng biệt → `CHANNEL_REMINDER`, `CHANNEL_BUDGET`, `CHANNEL_DEBT`

**Files:** `NotificationHelper.kt`, `ReminderReceiver.kt`, `BootReceiver.kt`  
**Độ khó:** ⭐⭐⭐  

---

### 1.3 🔐 App Lock (Bảo mật) ❌ CHƯA LÀM
- [ ] Khóa app bằng PIN 4-6 số
- [ ] Hỗ trợ Biometric (vân tay, Face ID)
- [ ] Toggle bật/tắt trong Settings
- [ ] Auto-lock khi app về background

**Độ khó:** ⭐⭐⭐  
**Thời gian ước tính:** 4-5 giờ

---

### 1.4 📱 Home Screen Widget ✅ HOÀN THÀNH
- [x] Widget hiển thị số dư tổng
- [x] Widget chi tiêu hôm nay/tháng này
- [x] Quick Add button trên widget
- [x] Auto-update khi có giao dịch mới

**File:** `ExpenseWidgetProvider.kt`  
**Độ khó:** ⭐⭐⭐  

---

### 1.5 🔄 Auto Recurring Transactions ❌ CHƯA LÀM
- [ ] Cấu hình tần suất: daily, weekly, monthly
- [ ] Cấu hình ngày thực hiện
- [ ] WorkManager để tự động tạo giao dịch
- [ ] Notification khi tạo xong
- [ ] Quản lý danh sách recurring

**Độ khó:** ⭐⭐⭐⭐  
**Thời gian ước tính:** 6-8 giờ

---

## 🎯 Phase 2: Tính năng mới (Ưu tiên trung bình)

### 2.1 🔍 Tìm kiếm nâng cao ❌ CHƯA LÀM
- [ ] Search bar trong màn hình chính
- [ ] Tìm theo note, category, amount
- [ ] Filter theo khoảng thời gian
- [ ] Lịch sử tìm kiếm

**Độ khó:** ⭐⭐  
**Thời gian ước tính:** 3-4 giờ

---

### 2.2 📸 Đính kèm hóa đơn ❌ CHƯA LÀM
- [ ] Chụp ảnh/chọn từ gallery
- [ ] Lưu ảnh trong app storage
- [ ] Thêm field `receiptPath` vào TransactionEntity
- [ ] Xem ảnh trong chi tiết giao dịch
- [ ] Xóa ảnh khi xóa giao dịch

**Độ khó:** ⭐⭐⭐  
**Thời gian ước tính:** 4-5 giờ

---

### 2.3 🏷️ Tags / Labels ✅ HOÀN THÀNH
- [x] Thêm TagEntity (id, name, color)
- [x] Many-to-many relationship với Transaction → `TransactionTagCrossRef`
- [x] UI chọn tags khi thêm giao dịch → `AddTransactionActivity`
- [x] Filter theo tags → `MainFragment`
- [x] Quản lý tags trong Settings → `ManageTagsActivity`

**Files:** `TagEntity.kt`, `TagDao.kt`, `TransactionTagCrossRef.kt`, `ManageTagsActivity.kt`  
**Độ khó:** ⭐⭐⭐  

---

### 2.4 📊 Báo cáo chi tiết ❌ CHƯA LÀM
- [ ] So sánh chi tiêu giữa các tháng
- [ ] Xu hướng thu/chi theo thời gian
- [ ] Top categories chi tiêu nhiều nhất
- [ ] Trung bình chi tiêu/ngày
- [ ] Export báo cáo PDF

**Độ khó:** ⭐⭐⭐  
**Thời gian ước tính:** 6-8 giờ

---

### 2.5 🎯 Mục tiêu tiết kiệm (Savings Goals) ❌ CHƯA LÀM
- [ ] SavingsGoalEntity (name, targetAmount, currentAmount, deadline)
- [ ] UI tạo/quản lý goals
- [ ] Progress bar visual
- [ ] Liên kết giao dịch "Gửi tiết kiệm" với goal
- [ ] Notification khi đạt goal

**Độ khó:** ⭐⭐⭐⭐  
**Thời gian ước tính:** 6-8 giờ

---

### 2.6 💱 Tỷ giá live ❌ CHƯA LÀM
- [ ] API lấy tỷ giá VND/USD realtime
- [ ] Hiển thị tỷ giá trong Settings
- [ ] Tự động convert khi đổi currency
- [ ] Cache tỷ giá offline

**Độ khó:** ⭐⭐⭐  
**Thời gian ước tính:** 3-4 giờ

---

## ☁️ Phase 3: Cloud & Sync (Ưu tiên thấp)

### 3.1 Google Drive Backup hoàn chỉnh ⚠️ MỘT PHẦN
- [x] DriveServiceHelper cơ bản → `DriveServiceHelper.kt`
- [ ] Auto backup hàng ngày (WorkManager)
- [ ] Restore từ Drive
- [ ] Conflict resolution
- [ ] UI hiển thị backup history

**Độ khó:** ⭐⭐⭐⭐  
**Thời gian ước tính:** 8-10 giờ

---

### 3.2 Firebase Realtime Sync ❌ CHƯA LÀM
- [ ] Hoàn thiện FirebaseUtils
- [ ] Sync data giữa các devices
- [ ] Offline-first với sync khi online
- [ ] Handle conflicts

**Độ khó:** ⭐⭐⭐⭐⭐  
**Thời gian ước tính:** 10-12 giờ

---

### 3.3 👥 Multi-user / Chia sẻ ❌ CHƯA LÀM
- [ ] Tạo nhóm gia đình
- [ ] Chia sẻ giao dịch trong nhóm
- [ ] Phân quyền view/edit
- [ ] Thống kê theo người

**Độ khó:** ⭐⭐⭐⭐⭐  
**Thời gian ước tính:** 15-20 giờ

---

## 🤖 Phase 4: AI Features (Tính năng AI)

### 4.1 🧠 AI Spending Insights ❌ CHƯA LÀM
- [ ] Phân tích pattern chi tiêu
- [ ] Gợi ý tiết kiệm dựa trên habits
- [ ] Dự đoán chi tiêu tháng tới
- [ ] Cảnh báo chi tiêu bất thường

**Công nghệ:** Gemini API / On-device ML  
**Độ khó:** ⭐⭐⭐⭐  
**Thời gian ước tính:** 8-10 giờ

---

### 4.2 📝 Smart Note với AI ❌ CHƯA LÀM
- [ ] Auto-suggest category từ note
- [ ] OCR scan hóa đơn tự nhập
- [ ] Voice input ghi chép bằng giọng nói
- [ ] Auto-extract amount từ text

**Công nghệ:** ML Kit, Speech Recognition  
**Độ khó:** ⭐⭐⭐⭐  
**Thời gian ước tính:** 10-12 giờ

---

### 4.3 💬 AI Chatbot Assistant ❌ CHƯA LÀM
- [ ] Hỏi đáp về chi tiêu bằng ngôn ngữ tự nhiên
- [ ] "Tháng này tôi chi bao nhiêu cho ăn uống?"
- [ ] "So sánh chi tiêu tháng này với tháng trước"
- [ ] Gợi ý cách tiết kiệm

**Công nghệ:** Gemini API / Dialogflow  
**Độ khó:** ⭐⭐⭐⭐⭐  
**Thời gian ước tính:** 15-20 giờ

---

### 4.4 📊 AI Budget Recommendation ❌ CHƯA LÀM
- [ ] Tự động đề xuất ngân sách dựa trên thu nhập
- [ ] Áp dụng quy tắc 50/30/20
- [ ] Điều chỉnh theo lịch sử chi tiêu
- [ ] Alert khi budget không phù hợp

**Công nghệ:** On-device calculation + Gemini  
**Độ khó:** ⭐⭐⭐  
**Thời gian ước tính:** 5-6 giờ

---

## 🛠️ Phase 5: Nâng cấp Architecture

### 5.1 Hilt Dependency Injection ❌ CHƯA LÀM
- [ ] Setup Hilt trong project
- [ ] Migrate AppDatabase sang @Singleton
- [ ] Inject Repositories vào ViewModels
- [ ] Xóa các ViewModelFactory thủ công

**Độ khó:** ⭐⭐⭐  
**Thời gian ước tính:** 4-5 giờ

---

### 5.2 DataStore thay SharedPreferences ❌ CHƯA LÀM
- [ ] Setup DataStore dependencies
- [ ] Migrate spending limit sang DataStore
- [ ] Migrate currency preference
- [ ] Migrate dark mode preference

**Độ khó:** ⭐⭐  
**Thời gian ước tính:** 2-3 giờ

---

### 5.3 Unit Tests ❌ CHƯA LÀM
- [ ] Setup testing dependencies
- [ ] Test MainViewModel
- [ ] Test BudgetViewModel
- [ ] Test ExpenseRepository
- [ ] Test CurrencyUtils

**Độ khó:** ⭐⭐⭐  
**Thời gian ước tính:** 6-8 giờ

---

### 5.4 Modularization ❌ CHƯA LÀM
- [ ] Tách module :core:database
- [ ] Tách module :core:ui
- [ ] Tách module :feature:transaction
- [ ] Tách module :feature:budget
- [ ] Tách module :feature:settings

**Độ khó:** ⭐⭐⭐⭐⭐  
**Thời gian ước tính:** 15-20 giờ

---

## 🎨 Phase 6: UI/UX Enhancements

### 6.1 🎨 Themes & Colors ❌ CHƯA LÀM
- [ ] Multiple color themes
- [ ] Dynamic Colors (Material You)
- [ ] Custom accent color picker

**Độ khó:** ⭐⭐  
**Thời gian ước tính:** 3-4 giờ

---

### 6.2 📱 Tablet Layout ❌ CHƯA LÀM
- [ ] Two-pane layout cho tablet
- [ ] Adaptive navigation
- [ ] Optimized charts cho màn hình lớn

**Độ khó:** ⭐⭐⭐  
**Thời gian ước tính:** 5-6 giờ

---

### 6.3 🌐 Multi-language ❌ CHƯA LÀM
- [ ] Tách strings sang resources
- [ ] Thêm tiếng Anh
- [ ] Language picker trong Settings

**Độ khó:** ⭐⭐  
**Thời gian ước tính:** 3-4 giờ

---

## 📋 Tổng kết

| Phase | Tổng | ✅ Xong | ⚠️ Một phần | ❌ Chưa | Tiến độ |
|-------|------|---------|-------------|---------|---------|
| Phase 1 (UX) | 5 | 3 | 0 | 2 | **60%** |
| Phase 2 (Features) | 6 | 1 | 0 | 5 | **17%** |
| Phase 3 (Cloud) | 3 | 0 | 1 | 2 | **~5%** |
| Phase 4 (AI) | 4 | 0 | 0 | 4 | **0%** |
| Phase 5 (Architecture) | 4 | 0 | 0 | 4 | **0%** |
| Phase 6 (UI/UX) | 3 | 0 | 0 | 3 | **0%** |
| **Tổng** | **25** | **4** | **1** | **20** | **~18%** |

> **Bonus:** 5 tính năng ngoài roadmap đã hoàn thành (Multi-Wallet, Planned Expenses, Services Page, Premium Charts, Notification Center)

---

## 🏁 Đề xuất thứ tự tiếp theo

1. 🔐 **App Lock** - Bảo mật quan trọng, chưa có
2. 🔍 **Search** - UX cần thiết, dễ làm (⭐⭐)
3. 🔄 **Auto Recurring** - Automation, tăng tiện ích
4. 📸 **Đính kèm hóa đơn** - Hay, impact cao
5. 🎯 **Savings Goals** - Tính năng hấp dẫn
6. 💱 **Tỷ giá live** - Tiện ích nhỏ

---

## 📝 Ghi chú

- Các tính năng có thể thay đổi dựa trên feedback
- Thời gian ước tính có thể thay đổi tùy độ phức tạp thực tế
- Ưu tiên có thể điều chỉnh theo nhu cầu người dùng
