package com.nhattien.expensemanager.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.domain.Category
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val transaction = MutableLiveData<TransactionEntity?>()

    // CẬP NHẬT: Thêm tham số 'date' và 'isRecurring'
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: Category,
        note: String?,
        date: Long,             // Ngày được chọn từ DatePicker
        isRecurring: Boolean    // Trạng thái Switch "Cố định tháng"
    ) {
        val entity = TransactionEntity(
            amount = amount,
            type = type,
            category = category,
            note = note ?: "",
            date = date,           // Lưu đúng ngày đã chọn
            isRecurring = isRecurring // Lưu trạng thái lặp lại
        )

        viewModelScope.launch {
            repository.insertTransaction(entity)
        }
    }
}