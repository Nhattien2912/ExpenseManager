package com.nhattien.expensemanager.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nhattien.expensemanager.ui.daydetail.DayDetailViewModel

class DayDetailViewModelFactory (
    private val application: Application,
    private val startOfDay: Long,
    private val endOfDay: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DayDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DayDetailViewModel(
                application,
                startOfDay,
                endOfDay
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}