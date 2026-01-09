package com.nhattien.expensemanager.utils

import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {

    // Format chuẩn quốc tế (tự động theo ngôn ngữ máy)
    // Ví dụ máy tiếng Việt: 100.000 ₫
    // Ví dụ máy tiếng Anh: 100,000 ₫ (hoặc $ tùy Locale)
    fun format(amount: Double): String {
        // Nếu muốn fix cứng tiền Việt cho người Việt ở nước ngoài thì dùng dòng này:
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        // Còn nếu muốn tự động theo máy (Đa ngôn ngữ) thì dùng dòng dưới (Bỏ comment):
        // val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())

        return formatter.format(amount)
    }

    // Format rút gọn cho Lịch (100k, 1Tr...) - Giữ nguyên logic này vì nó gọn đẹp
    fun formatShort(amount: Double): String {
        val absAmount = Math.abs(amount)
        return when {
            absAmount >= 1_000_000_000 -> "${(amount / 1_000_000_000).toInt()}B" // Tỷ
            absAmount >= 1_000_000 -> "${(amount / 1_000_000).toInt()}Tr"
            absAmount >= 1_000 -> "${(amount / 1_000).toInt()}k"
            else -> "${amount.toInt()}"
        }
    }
}