package com.nhattien.expensemanager.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.domain.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

import com.nhattien.expensemanager.data.entity.TransactionWithCategory

class TransactionAdapter(
    private val onItemClick: (TransactionEntity) -> Unit // Keep clicking returning entity for simpler logic usually, or wrapper? Let's return Entity as most operations are on Transaction.
) : ListAdapter<TransactionWithCategory, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtCategoryIcon: TextView = itemView.findViewById(R.id.txtCategoryIcon)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        private val txtNote: TextView = itemView.findViewById(R.id.txtNote)
        private val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)
        private val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        private val icRecurring: ImageView = itemView.findViewById(R.id.icRecurring)

        fun bind(item: TransactionWithCategory) {
            val transaction = item.transaction
            val category = item.category

            // Dynamic Category from DB
            txtCategoryIcon.text = category.icon
            txtTitle.text = category.name

            // Hiển thị ghi chú
            txtNote.text = if (transaction.note.isNotEmpty()) transaction.note else "Không có ghi chú"
            txtNote.visibility = if (transaction.note.isNotEmpty()) View.VISIBLE else View.GONE // Optional: Hide if empty? Keep consistent.

            // Hiển thị ngày tháng thông minh
            val now = java.util.Calendar.getInstance()
            val itemTime = java.util.Calendar.getInstance().apply { timeInMillis = transaction.date }
            
            val isToday = now.get(java.util.Calendar.YEAR) == itemTime.get(java.util.Calendar.YEAR) &&
                          now.get(java.util.Calendar.DAY_OF_YEAR) == itemTime.get(java.util.Calendar.DAY_OF_YEAR)
            
            val pattern = when {
                isToday -> "'Hôm nay' HH:mm"
                else -> "dd/MM/yyyy HH:mm"
            }
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            txtDate.text = sdf.format(transaction.date)

            // Hiển thị icon lặp lại
            icRecurring.visibility = if (transaction.isRecurring) View.VISIBLE else View.GONE

            // Định dạng tiền và màu sắc
            val amountStr = com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(transaction.amount)
            
            // Show Payment Method (New Feature)
            // Can append to Date or Title? 
            // e.g. "Hôm nay 10:00 • Tiền mặt"
            val paymentMethod = if (transaction.paymentMethod == "BANK") "Chuyển khoản" else "Tiền mặt"
            txtDate.text = "${sdf.format(transaction.date)} • $paymentMethod"

            when (transaction.type) {
                TransactionType.INCOME, TransactionType.LOAN_TAKE -> {
                    txtAmount.text = "+ $amountStr"
                    txtAmount.setTextColor(Color.parseColor("#4CAF50"))
                }
                TransactionType.EXPENSE, TransactionType.LOAN_GIVE -> {
                    txtAmount.text = "- $amountStr"
                    txtAmount.setTextColor(Color.parseColor("#F44336"))
                }
            }

            itemView.setOnClickListener { onItemClick(transaction) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionWithCategory>() {
        override fun areItemsTheSame(oldItem: TransactionWithCategory, newItem: TransactionWithCategory) = oldItem.transaction.id == newItem.transaction.id
        override fun areContentsTheSame(oldItem: TransactionWithCategory, newItem: TransactionWithCategory) = oldItem == newItem
    }
}
