package com.nhattien.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhattien.expensemanager.data.entity.WalletEntity
import com.nhattien.expensemanager.data.repository.ExpenseRepository
import com.nhattien.expensemanager.domain.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WalletViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val allWallets = repository.allWallets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val walletBalances = combine(allWallets, repository.allTransactions) { wallets, transactions ->
        val balances = wallets.associate { it.id to it.initialBalance }.toMutableMap()

        transactions.forEach { item ->
            val tx = item.transaction

            fun applyDelta(walletId: Long, delta: Double) {
                if (balances.containsKey(walletId)) {
                    balances[walletId] = (balances[walletId] ?: 0.0) + delta
                }
            }

            when (tx.type) {
                TransactionType.INCOME, TransactionType.LOAN_TAKE -> applyDelta(tx.walletId, tx.amount)
                TransactionType.EXPENSE, TransactionType.LOAN_GIVE -> applyDelta(tx.walletId, -tx.amount)
                TransactionType.TRANSFER -> {
                    applyDelta(tx.walletId, -tx.amount)
                    tx.targetWalletId?.let { applyDelta(it, tx.amount) }
                }
            }
        }

        balances.toMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

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

    fun isWalletNameUsed(name: String, excludeWalletId: Long? = null): Boolean {
        val normalized = name.trim().lowercase()
        if (normalized.isEmpty()) return false
        return allWallets.value.any {
            it.id != excludeWalletId && it.name.trim().lowercase() == normalized
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
