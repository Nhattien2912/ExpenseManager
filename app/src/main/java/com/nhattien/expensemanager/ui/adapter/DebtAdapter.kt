package com.nhattien.expensemanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.TransactionEntity
import com.nhattien.expensemanager.domain.Category
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat

class DebtAdapter(
    private val onSettleClick: (TransactionEntity) -> Unit
) : ListAdapter<TransactionEntity, DebtAdapter.DebtViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_debt, parent, false)
        return DebtViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DebtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtBorrowerName)
        private val txtDate: TextView = itemView.findViewById(R.id.txtDueDate)
        private val txtAmount: TextView = itemView.findViewById(R.id.txtDebtAmount)
        private val btnSettle: View = itemView.findViewById(R.id.btnPaid)

        fun bind(item: TransactionEntity) {
            // Note often contains "Cho [Name] vay" or just "[Name]"
            txtName.text = item.note.ifEmpty { "Khoản nợ không tên" }
            
            // val formatter = java.text.NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            // val formattedAmount = formatter.format(item.amount).replace("₫", "đ")
            val formattedAmount = com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(item.amount)
            txtAmount.text = "Số tiền: $formattedAmount"

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            txtDate.text = "Ngày: ${sdf.format(Date(item.date))}"
            txtDate.setTextColor(0xFF757575.toInt()) // Reset color

            // Button Text/Visibility
            // Note: item_debt might have specific text like "Đã trả"
            // We keep it simple. Settle creates a counter transaction.
            
            btnSettle.setOnClickListener { onSettleClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TransactionEntity, newItem: TransactionEntity) = oldItem == newItem
    }
}
