package com.nhattien.expensemanager.domain

import com.nhattien.expensemanager.R

// Định nghĩa các nhóm để dễ lọc hiển thị
enum class TypeGroup {
    EXPENSE_FIXED, // Chi cố định (Điện, nước, nhà...)
    EXPENSE_DAILY, // Chi linh hoạt (Ăn, chơi, xăng...)
    INCOME,        // Thu nhập
    DEBT,          // Vay/Nợ
    SAVING         // Tiết kiệm
}

enum class Category(
    val label: String,
    val iconRes: Int,
    val group: TypeGroup
) {
    // === 1. CHI TIÊU CỐ ĐỊNH (FIXED) ===
    RENT("Tiền nhà", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_FIXED),
    ELECTRICITY("Tiền điện", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_FIXED),
    WATER("Tiền nước", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_FIXED),
    INTERNET("Wifi/3G", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_FIXED),
    SEND_HOME("Gửi về nhà", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_FIXED),
    BANK_FEE("Phí Ngân hàng", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_FIXED),

    // === 2. CHI TIÊU HẰNG NGÀY (DAILY) ===
    FOOD("Ăn uống", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),
    COFFEE("Cafe/Trà", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),
    MARKET("Đi chợ", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),
    SHOPPING("Mua sắm Online", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),
    GAS("Xăng xe", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),
    ENTERTAINMENT("Xem phim", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),
    GAME("Nạp game", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),
    CIGARETTE("Thuốc lá", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),
    DATING("Hẹn hò/Gái gú", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY), // :))
    MEDICINE("Thuốc men", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),
    OTHER_EXPENSE("Chi khác", R.drawable.ic_launcher_foreground, TypeGroup.EXPENSE_DAILY),

    // === 3. THU NHẬP (INCOME) ===
    SALARY("Lương Cty", R.drawable.ic_wallet, TypeGroup.INCOME),
    BONUS("Thưởng", R.drawable.ic_wallet, TypeGroup.INCOME),
    PART_TIME("Làm thêm", R.drawable.ic_wallet, TypeGroup.INCOME),
    INTEREST("Lãi Ngân hàng", R.drawable.ic_wallet, TypeGroup.INCOME), // Nhận lãi
    GIFT("Được tặng", R.drawable.ic_wallet, TypeGroup.INCOME),

    // === 4. VAY / NỢ (DEBT) ===
    LENDING("Cho vay", R.drawable.ic_wallet, TypeGroup.DEBT),         // Tiền đi
    DEBT_COLLECTION("Thu nợ", R.drawable.ic_wallet, TypeGroup.DEBT),  // Tiền về
    BORROWING("Đi vay", R.drawable.ic_wallet, TypeGroup.DEBT),        // Tiền về
    DEBT_REPAYMENT("Trả nợ", R.drawable.ic_wallet, TypeGroup.DEBT),   // Tiền đi
    PAY_INTEREST("Trả lãi vay", R.drawable.ic_wallet, TypeGroup.DEBT), // Tiền đi

    // === 5. TIẾT KIỆM (SAVING) ===
    SAVING_IN("Gửi tiết kiệm", R.drawable.ic_wallet, TypeGroup.SAVING), // Coi như chi ra khỏi ví
    SAVING_OUT("Rút tiết kiệm", R.drawable.ic_wallet, TypeGroup.SAVING); // Coi như thu vào ví
}