package com.nhattien.expensemanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.WalletEntity
import com.nhattien.expensemanager.utils.CurrencyUtils

class WalletAdapter(
    private val onWalletClick: (WalletEntity) -> Unit
) : ListAdapter<WalletEntity, WalletAdapter.WalletViewHolder>(DiffCallback()) {

    // Helper to inject calculated balance if needed
    // But for now, WalletEntity only has initialBalance. 
    // Real balance calculation is complex (initial + income - expense).
    // Ideally, we pass a data class `WalletWithBalance`.
    // For MVP, we might just show initialBalance or we need to calculate it.
    // Let's assume for now we just show name and look. 
    // Wait, user wants to see Balance. 
    // The ViewModel needs to provide a list of Wallets WITH Current Balance.
    
    // Changing approach: Adapter should bind WalletEntity, but how to get balance?
    // Maybe we pass a Map<Long, Double> of balances?
    // Or we update WalletEntity to include `currentBalance` (ignored by Room)?
    // Let's us a simple map for now.
    
    var balances: Map<Long, Double> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet, parent, false)
        return WalletViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        val wallet = getItem(position)
        val balance = balances[wallet.id] ?: wallet.initialBalance
        holder.bind(wallet, balance)
    }

    inner class WalletViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtIcon: TextView = itemView.findViewById(R.id.txtIcon)
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtBalance: TextView = itemView.findViewById(R.id.txtBalance)

        fun bind(wallet: WalletEntity, balance: Double) {
            txtIcon.text = wallet.icon
            
            // Set background tint to wallet color
            txtIcon.background.setTint(wallet.color)
            // Ensure contrast or use white icon on color bg
            // txtIcon.setTextColor(Color.WHITE) // Already set in XML

            txtName.text = wallet.name
            
            txtBalance.text = CurrencyUtils.toCurrency(balance)
            
            itemView.setOnClickListener { onWalletClick(wallet) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WalletEntity>() {
        override fun areItemsTheSame(oldItem: WalletEntity, newItem: WalletEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WalletEntity, newItem: WalletEntity) = oldItem == newItem
    }
}
