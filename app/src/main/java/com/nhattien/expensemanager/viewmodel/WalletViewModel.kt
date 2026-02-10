package com.nhattien.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.entity.WalletEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val allWallets = repository.allWallets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertWallet(name: String, initialBalance: Double, icon: String, color: Int) {
        viewModelScope.launch {
            val wallet = WalletEntity(
                name = name,
                initialBalance = initialBalance,
                icon = icon,
                color = color
            )
            repository.insertWallet(wallet)
        }
    }

    fun updateWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            repository.updateWallet(wallet)
        }
    }

    fun deleteWallet(wallet: WalletEntity) {
        viewModelScope.launch {
            // Check if default wallet? Maybe prevent deleting default wallet (ID 1)
            if (wallet.id == 1L) {
                // Prevent or Warn. For now, let's just archive if user tries to delete.
                // Or allows repository to handle constraint.
                // For safety:
                return@launch
            }
            // repository.deleteWallet(wallet) // Hard delete
            repository.archiveWallet(wallet.id) // Soft delete prefered
        }
    }
}
