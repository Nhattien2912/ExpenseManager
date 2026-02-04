---
name: ExpenseManager Currency Utils
description: Utility functions để format và parse tiền tệ VND/USD trong ExpenseManager.
---

# Currency Utils trong ExpenseManager

## Sử dụng CurrencyUtils

### Format tiền tệ đầy đủ
```kotlin
import com.nhattien.expensemanager.utils.CurrencyUtils

// VND: 1.500.000 đ, USD: $1,500.00
val formatted = CurrencyUtils.toCurrency(1500000.0)
```

### Format với dấu phân cách (không có ký hiệu tiền tệ)
```kotlin
// 1.500.000
val formatted = CurrencyUtils.formatWithSeparator(1500000.0)
```

### Format rút gọn cho biểu đồ
```kotlin
// 1.5M, 500K, 50
val short = CurrencyUtils.formatShort(1500000.0)
```

### Parse từ text input
```kotlin
// "1.500.000" -> 1500000.0
val amount = CurrencyUtils.parseFromSeparator(text)
```

## TextWatcher cho EditText

```kotlin
// Tự động format số khi nhập
binding.edtAmount.addTextChangedListener(
    CurrencyUtils.MoneyTextWatcher(binding.edtAmount)
)

// Lấy giá trị số
val amount = CurrencyUtils.parseFromSeparator(binding.edtAmount.text.toString())
```

## Chuyển đổi tiền tệ

```kotlin
// Chuyển sang VND
CurrencyUtils.checkCurrency = 0

// Chuyển sang USD
CurrencyUtils.checkCurrency = 1
```

## Giới hạn

```kotlin
// Số tiền tối đa: 999 tỷ
CurrencyUtils.MAX_AMOUNT = 999_999_999_999.0

// Số chữ số tối đa: 12
CurrencyUtils.MAX_DIGITS = 12
```

## Hiển thị trong Adapter

```kotlin
val amountStr = CurrencyUtils.toCurrency(transaction.amount)
when (transaction.type) {
    TransactionType.INCOME -> {
        txtAmount.text = "+ $amountStr"
        txtAmount.setTextColor(Color.parseColor("#4CAF50"))
    }
    TransactionType.EXPENSE -> {
        txtAmount.text = "- $amountStr"
        txtAmount.setTextColor(Color.parseColor("#F44336"))
    }
}
```
