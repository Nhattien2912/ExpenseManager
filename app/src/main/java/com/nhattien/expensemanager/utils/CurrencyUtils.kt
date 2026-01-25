package com.nhattien.expensemanager.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    var checkCurrency: Int = 0 // 0: VND, 1: USD
    
    // Giới hạn số tiền tối đa (999 tỷ)
    const val MAX_AMOUNT = 999_999_999_999.0
    
    // Số chữ số tối đa
    const val MAX_DIGITS = 12

    fun toCurrency(amount: Double): String {
        // Giới hạn số tiền để tránh overflow
        val safeAmount = amount.coerceIn(-MAX_AMOUNT, MAX_AMOUNT)
        return if (checkCurrency == 0) {
            val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            format.format(safeAmount).replace("₫", "đ")
        } else {
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.format(safeAmount)
        }
    }
    
    /**
     * Format số với dấu phân cách hàng nghìn (.)
     * e.g., 1500000 -> "1.500.000"
     */
    fun formatWithSeparator(amount: Double): String {
        val safeAmount = amount.coerceIn(-MAX_AMOUNT, MAX_AMOUNT)
        val symbols = DecimalFormatSymbols().apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(safeAmount)
    }
    
    /**
     * Parse số từ chuỗi có dấu phân cách
     * e.g., "1.500.000" -> 1500000.0
     */
    fun parseFromSeparator(text: String): Double {
        return try {
            text.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Format currency in short form for charts
     * e.g., 1500000 -> "1.5M", 50000 -> "50K"
     */
    fun formatShort(amount: Double): String {
        val safeAmount = amount.coerceIn(-MAX_AMOUNT, MAX_AMOUNT)
        return when {
            kotlin.math.abs(safeAmount) >= 1_000_000_000 -> String.format("%.1fB", safeAmount / 1_000_000_000)
            kotlin.math.abs(safeAmount) >= 1_000_000 -> String.format("%.1fM", safeAmount / 1_000_000)
            kotlin.math.abs(safeAmount) >= 1_000 -> String.format("%.0fK", safeAmount / 1_000)
            else -> String.format("%.0f", safeAmount)
        }
    }
    
    /**
     * TextWatcher để tự động format số khi nhập
     */
    class MoneyTextWatcher(private val editText: EditText) : TextWatcher {
        private var current = ""
        
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        
        override fun afterTextChanged(s: Editable?) {
            if (s.toString() != current) {
                editText.removeTextChangedListener(this)
                
                // Lấy số thuần không có dấu phân cách
                val cleanString = s.toString().replace(".", "").replace(",", "")
                
                // Giới hạn số chữ số
                val limitedString = if (cleanString.length > MAX_DIGITS) {
                    cleanString.substring(0, MAX_DIGITS)
                } else {
                    cleanString
                }
                
                // Format lại với dấu phân cách
                if (limitedString.isNotEmpty()) {
                    val parsed = limitedString.toLongOrNull() ?: 0L
                    val formatted = formatWithSeparator(parsed.toDouble())
                    current = formatted
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                } else {
                    current = ""
                    editText.setText("")
                }
                
                editText.addTextChangedListener(this)
            }
        }
    }
}
