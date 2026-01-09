package com.nhattien.expensemanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.DebtEntity
import com.nhattien.expensemanager.utils.MoneyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebtAdapter(
    private val onSettleClick: (DebtEntity) -> Unit
) : ListAdapter<DebtEntity, DebtAdapter.DebtViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_debt, parent, false)
        return DebtViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DebtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtDebtorName)
        private val txtDate: TextView = itemView.findViewById(R.id.txtDueDate)
        private val txtAmount: TextView = itemView.findViewById(R.id.txtAmountDebt)
        private val btnSettle: View = itemView.findViewById(R.id.btnSettle)

        fun bind(item: DebtEntity) {
            txtName.text = item.debtorName
            txtAmount.text = MoneyUtils.format(item.amount)

            if (item.dueDate != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                txtDate.text = "Hạn: ${sdf.format(Date(item.dueDate))}"

                // Logic báo đỏ nếu quá hạn
                if (item.dueDate < System.currentTimeMillis() && !item.isFinished) {
                    txtDate.setTextColor(0xFFD32F2F.toInt()) // Màu đỏ
                    txtDate.text = "${txtDate.text} (QUÁ HẠN)"
                }
            } else {
                txtDate.text = "Không có thời hạn"
            }

            btnSettle.setOnClickListener { onSettleClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DebtEntity>() {
        override fun areItemsTheSame(oldItem: DebtEntity, newItem: DebtEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DebtEntity, newItem: DebtEntity) = oldItem == newItem
    }
}