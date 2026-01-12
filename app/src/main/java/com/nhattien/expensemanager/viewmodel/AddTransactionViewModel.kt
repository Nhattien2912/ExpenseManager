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
        isRecurring: Boolean,   // Trạng thái Switch "Cố định tháng"
        onSuccess: () -> Unit
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
            onSuccess()
        }
    }

    // CẬP NHẬT: Thêm hàm update
    fun updateTransaction(
        id: Long,
        amount: Double,
        type: TransactionType,
        category: Category,
        note: String?,
        date: Long,
        isRecurring: Boolean,
        onSuccess: () -> Unit
    ) {
        val entity = TransactionEntity(
            id = id,
            amount = amount,
            type = type,
            category = category,
            note = note ?: "",
            date = date,
            isRecurring = isRecurring
        )
        viewModelScope.launch {
            repository.updateTransaction(entity)
            onSuccess()
        }
    }

    fun getTransaction(id: Long) {
        viewModelScope.launch {
            val result = repository.getTransactionById(id)
            transaction.postValue(result)
        }
    }
}