package com.nhattien.expensemanager.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    var checkCurrency: Int = 0 // 0: VND, 1: USD

    fun toCurrency(amount: Double): String {
        return if (checkCurrency == 0) {
            val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            format.format(amount).replace("₫", "đ")
        } else {
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.format(amount)
        }
    }
}
