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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
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
        private val txtCategory: TextView = itemView.findViewById(R.id.txtCategoryName)
        private val txtNote: TextView = itemView.findViewById(R.id.txtNote)
        private val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)
        private val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        private val icRecurring: ImageView = itemView.findViewById(R.id.icRecurring)
        private val imgCategory: ImageView = itemView.findViewById(R.id.imgCategory) // Đã thêm ID này vào XML

        fun bind(item: TransactionEntity) {
            txtCategory.text = item.category.label
            imgCategory.setImageResource(item.category.iconRes)

            // Hiển thị ghi chú
            txtNote.text = if (item.note.isNotEmpty()) item.note else item.category.label

            // Xử lý hiển thị ngày tháng thông minh
            val today = System.currentTimeMillis()
            val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

            val isSameDay = sdfDate.format(today) == sdfDate.format(item.date)

            if (isSameDay) {
                txtDate.text = "Hôm nay ${sdfTime.format(item.date)}"
                txtDate.setTextColor(Color.parseColor("#2196F3")) // Màu xanh cho hôm nay
            } else {
                txtDate.text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(item.date)
                txtDate.setTextColor(Color.parseColor("#757575"))
            }

            // Icon Cố định (Vòng xoay)
            if (item.isRecurring) {
                icRecurring.visibility = View.VISIBLE
            } else {
                icRecurring.visibility = View.GONE
            }

            // Dùng MoneyUtils mới để format tiền tệ chuẩn
            val amountStr = com.nhattien.expensemanager.utils.MoneyUtils.format(item.amount)

            when (item.type) {
                TransactionType.INCOME -> {
                    txtAmount.text = "+ $amountStr"
                    txtAmount.setTextColor(Color.parseColor("#4CAF50"))
                }
                TransactionType.EXPENSE -> {
                    txtAmount.text = "- $amountStr"
                    txtAmount.setTextColor(Color.parseColor("#F44336"))
                }
                // ... (Các case khác giữ nguyên logic màu)
                TransactionType.LOAN_GIVE -> {
                    txtAmount.text = "- $amountStr"
                    txtAmount.setTextColor(Color.parseColor("#FF9800"))
                }
                TransactionType.LOAN_TAKE -> {
                    txtAmount.text = "+ $amountStr"
                    txtAmount.setTextColor(Color.parseColor("#2196F3"))
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity) = oldItem == newItem
    }
}