package com.nhattien.expensemanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.utils.CurrencyUtils
import com.nhattien.expensemanager.viewmodel.SavingsBucketItem

class SavingsBucketAdapter :
    ListAdapter<SavingsBucketItem, SavingsBucketAdapter.SavingsBucketViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsBucketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_savings_bucket, parent, false)
        return SavingsBucketViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavingsBucketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SavingsBucketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtBucketName: TextView = itemView.findViewById(R.id.txtBucketName)
        private val txtBucketCount: TextView = itemView.findViewById(R.id.txtBucketCount)
        private val txtBucketBalance: TextView = itemView.findViewById(R.id.txtBucketBalance)
        private val txtBucketIn: TextView = itemView.findViewById(R.id.txtBucketIn)
        private val txtBucketOut: TextView = itemView.findViewById(R.id.txtBucketOut)

        fun bind(item: SavingsBucketItem) {
            txtBucketName.text = item.name
            txtBucketCount.text = "${item.transactionCount} GD"
            txtBucketBalance.text = CurrencyUtils.toCurrency(item.balance)
            txtBucketIn.text = "+ ${CurrencyUtils.toCurrency(item.deposited)}"
            txtBucketOut.text = "- ${CurrencyUtils.toCurrency(item.withdrawn)}"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SavingsBucketItem>() {
        override fun areItemsTheSame(oldItem: SavingsBucketItem, newItem: SavingsBucketItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: SavingsBucketItem, newItem: SavingsBucketItem): Boolean {
            return oldItem == newItem
        }
    }
}
