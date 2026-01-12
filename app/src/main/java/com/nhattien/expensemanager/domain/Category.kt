package com.nhattien.expensemanager.domain

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
    val icon: String, // Đổi từ iconRes sang String để dùng Emoji
    val group: TypeGroup
) {
    // === 1. CHI TIÊU CỐ ĐỊNH (FIXED) ===
    RENT("Tiền nhà", "🏠", TypeGroup.EXPENSE_FIXED),
    ELECTRICITY("Tiền điện", "⚡", TypeGroup.EXPENSE_FIXED),
    WATER("Tiền nước", "💧", TypeGroup.EXPENSE_FIXED),
    INTERNET("Wifi/3G", "🌐", TypeGroup.EXPENSE_FIXED),
    SEND_HOME("Gửi về nhà", "👨‍👩‍👧‍👦", TypeGroup.EXPENSE_FIXED),
    BANK_FEE("Phí Ngân hàng", "🏦", TypeGroup.EXPENSE_FIXED),

    // === 2. CHI TIÊU HẰNG NGÀY (DAILY) ===
    FOOD("Ăn uống", "🍜", TypeGroup.EXPENSE_DAILY),
    COFFEE("Cafe/Trà", "☕", TypeGroup.EXPENSE_DAILY),
    MARKET("Đi chợ", "🛒", TypeGroup.EXPENSE_DAILY),
    SHOPPING("Mua sắm", "🛍️", TypeGroup.EXPENSE_DAILY),
    GAS("Xăng xe", "⛽", TypeGroup.EXPENSE_DAILY),
    ENTERTAINMENT("Giải trí", "🎮", TypeGroup.EXPENSE_DAILY),
    GAME("Nạp game", "🕹️", TypeGroup.EXPENSE_DAILY),
    CIGARETTE("Thuốc lá", "🚬", TypeGroup.EXPENSE_DAILY),
    DATING("Hẹn hò", "❤️", TypeGroup.EXPENSE_DAILY),
    MEDICINE("Thuốc men", "💊", TypeGroup.EXPENSE_DAILY),
    OTHER_EXPENSE("Chi khác", "📦", TypeGroup.EXPENSE_DAILY),

    // === 3. THU NHẬP (INCOME) ===
    SALARY("Lương Cty", "💰", TypeGroup.INCOME),
    BONUS("Thưởng", "🧧", TypeGroup.INCOME),
    PART_TIME("Làm thêm", "🛠️", TypeGroup.INCOME),
    INTEREST("Lãi Bank", "📈", TypeGroup.INCOME),
    GIFT("Được tặng", "🎁", TypeGroup.INCOME),

    // === 4. VAY / NỢ (DEBT) ===
    LENDING("Cho vay", "🤝", TypeGroup.DEBT),
    DEBT_COLLECTION("Thu nợ", "📥", TypeGroup.DEBT),
    BORROWING("Đi vay", "📤", TypeGroup.DEBT),
    DEBT_REPAYMENT("Trả nợ", "💸", TypeGroup.DEBT),
    PAY_INTEREST("Trả lãi", "🧾", TypeGroup.DEBT),

    // === 5. TIẾT KIỆM (SAVING) ===
    SAVING_IN("Gửi tiết kiệm", "🐷", TypeGroup.SAVING),
    SAVING_OUT("Rút tiết kiệm", "🔓", TypeGroup.SAVING);
}
