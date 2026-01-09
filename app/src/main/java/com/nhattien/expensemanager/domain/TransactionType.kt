package com.nhattien.expensemanager.domain

enum class TransactionType {
    INCOME,     // Thu nhập
    EXPENSE,    // Chi tiêu
    LOAN_GIVE,  // Cho người khác vay (Tiền mình giảm)
    LOAN_TAKE   // Đi vay người khác (Tiền mình tăng)
}