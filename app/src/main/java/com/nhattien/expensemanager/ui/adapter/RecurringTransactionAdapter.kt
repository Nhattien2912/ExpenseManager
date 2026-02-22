package com.nhattien.expensemanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.RecurringTransactionEntity
import com.nhattien.expensemanager.domain.TransactionType
import com.nhattien.expensemanager.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecurringTransactionAdapter(
    private val onDeleteClick: (RecurringTransactionEntity) -> Unit,
    private val onStatusChange: (Long, Boolean) -> Unit
) : ListAdapter<RecurringTransactionEntity, RecurringTransactionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recurring_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)
        private val txtNote: TextView = itemView.findViewById(R.id.txtNote)
        private val txtPeriod: TextView = itemView.findViewById(R.id.txtPeriod)
        private val txtNextRun: TextView = itemView.findViewById(R.id.txtNextRun)
        private val txtSource: TextView = itemView.findViewById(R.id.txtSource)
        private val txtProgress: TextView = itemView.findViewById(R.id.txtProgress)
        private val swActive: SwitchMaterial = itemView.findViewById(R.id.swActive)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
        private val imgCategory: ImageView = itemView.findViewById(R.id.imgCategory)

        fun bind(transaction: RecurringTransactionEntity) {
            val isIncome = transaction.type == TransactionType.INCOME
            val color = if (isIncome) R.color.income else R.color.expense
            txtAmount.setTextColor(itemView.context.getColor(color))
            val prefix = if (isIncome) "+" else "-"
            txtAmount.text = "$prefix ${CurrencyUtils.toCurrency(transaction.amount)}"

            txtNote.text = transaction.note.ifBlank { "Giao dịch định kỳ" }

            val periodNames = mapOf(
                "DAILY" to "Hàng ngày",
                "WEEKLY" to "Hàng tuần",
                "MONTHLY" to "Hàng tháng",
                "YEARLY" to "Hàng năm"
            )
            txtPeriod.text = periodNames[transaction.recurrencePeriod] ?: transaction.recurrencePeriod

            // Source badge
            if (transaction.loanSource == "BANK") {
                txtSource.visibility = View.VISIBLE
                txtSource.text = "🏦 Ngân hàng"
            } else {
                txtSource.visibility = View.GONE
            }
            
            // Installment progress
            if (transaction.totalInstallments > 0) {
                txtProgress.visibility = View.VISIBLE
                txtProgress.text = "Kỳ ${transaction.completedInstallments}/${transaction.totalInstallments}"
            } else {
                txtProgress.visibility = View.GONE
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            txtNextRun.text = "Tiếp theo: ${sdf.format(Date(transaction.nextRunDate))}"

            swActive.setOnCheckedChangeListener(null)
            swActive.isChecked = transaction.isActive
            swActive.setOnCheckedChangeListener { _, isChecked ->
                onStatusChange(transaction.id, isChecked)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(transaction)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RecurringTransactionEntity>() {
        override fun areItemsTheSame(oldItem: RecurringTransactionEntity, newItem: RecurringTransactionEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecurringTransactionEntity, newItem: RecurringTransactionEntity): Boolean {
            return oldItem == newItem
        }
    }
}
