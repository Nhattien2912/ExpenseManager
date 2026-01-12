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

class TransactionAdapter(
    private val onItemClick: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

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

        fun bind(item: TransactionEntity) {
            // SỬA TẠI ĐÂY: Dùng item.category.label thay vì item.category.name
            txtCategoryIcon.text = item.category.icon
            txtTitle.text = item.category.label

            // Hiển thị ghi chú
            txtNote.text = if (item.note.isNotEmpty()) item.note else "Không có ghi chú"

            // Hiển thị ngày tháng
            val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            txtDate.text = sdf.format(item.date)

            // Hiển thị icon lặp lại
            icRecurring.visibility = if (item.isRecurring) View.VISIBLE else View.GONE

            // Định dạng tiền và màu sắc
            val amountStr = com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(item.amount)
            
            when (item.type) {
                TransactionType.INCOME, TransactionType.LOAN_TAKE -> {
                    txtAmount.text = "+ $amountStr"
                    txtAmount.setTextColor(Color.parseColor("#4CAF50"))
                }
                TransactionType.EXPENSE, TransactionType.LOAN_GIVE -> {
                    txtAmount.text = "- $amountStr"
                    txtAmount.setTextColor(Color.parseColor("#F44336"))
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity) = oldItem == newItem
    }
}
