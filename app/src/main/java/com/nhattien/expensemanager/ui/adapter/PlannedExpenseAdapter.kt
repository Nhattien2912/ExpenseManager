package com.nhattien.expensemanager.ui.adapter

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.PlannedExpenseEntity
import com.nhattien.expensemanager.data.entity.WalletEntity
import com.nhattien.expensemanager.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Locale

class PlannedExpenseAdapter(
    private val onToggle: (PlannedExpenseEntity) -> Unit,
    private val onDelete: (PlannedExpenseEntity) -> Unit,
    private val onEdit: (PlannedExpenseEntity) -> Unit
) : ListAdapter<PlannedExpenseEntity, PlannedExpenseAdapter.ViewHolder>(DiffCallback()) {

    var categoryMap: Map<Long, CategoryEntity> = emptyMap()
    var walletMap: Map<Long, WalletEntity> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_planned_expense, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtPlanTitle)
        private val txtAmount: TextView = itemView.findViewById(R.id.txtPlanAmount)
        private val txtDueDate: TextView = itemView.findViewById(R.id.txtPlanDueDate)
        private val txtNote: TextView = itemView.findViewById(R.id.txtPlanNote)
        private val txtCategory: TextView = itemView.findViewById(R.id.txtPlanCategory)

        fun bind(item: PlannedExpenseEntity) {
            cbCompleted.setOnClickListener(null)
            cbCompleted.isChecked = item.isCompleted

            txtTitle.text = item.title
            txtAmount.text = CurrencyUtils.toCurrency(item.amount)

            val cat = categoryMap[item.categoryId]
            val wallet = walletMap[item.walletId]
            val infoBuilder = StringBuilder()
            if (cat != null) infoBuilder.append("${cat.icon} ${cat.name}")
            if (wallet != null) {
                if (infoBuilder.isNotEmpty()) infoBuilder.append(" - ")
                infoBuilder.append("${wallet.icon} ${wallet.name}")
            }
            if (infoBuilder.isNotEmpty()) {
                txtCategory.text = infoBuilder.toString()
                txtCategory.visibility = View.VISIBLE
            } else {
                txtCategory.visibility = View.GONE
            }

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            txtDueDate.text = "Ngày dự kiến: ${sdf.format(item.dueDate)}"

            if (item.note.isNotEmpty()) {
                txtNote.text = item.note
                txtNote.visibility = View.VISIBLE
            } else {
                txtNote.visibility = View.GONE
            }

            if (item.isCompleted) {
                txtTitle.paintFlags = txtTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                txtAmount.paintFlags = txtAmount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                txtTitle.setTextColor(Color.parseColor("#9E9E9E"))
                txtAmount.setTextColor(Color.parseColor("#4CAF50"))
                itemView.alpha = 0.6f
            } else {
                txtTitle.paintFlags = txtTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                txtAmount.paintFlags = txtAmount.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                txtTitle.setTextColor(Color.parseColor("#212121"))
                txtAmount.setTextColor(Color.parseColor("#F44336"))
                itemView.alpha = 1.0f
            }

            cbCompleted.setOnClickListener {
                cbCompleted.isChecked = item.isCompleted
                onToggle(item)
            }

            itemView.setOnClickListener {
                onEdit(item)
            }

            itemView.setOnLongClickListener {
                onDelete(item)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PlannedExpenseEntity>() {
        override fun areItemsTheSame(a: PlannedExpenseEntity, b: PlannedExpenseEntity) = a.id == b.id
        override fun areContentsTheSame(a: PlannedExpenseEntity, b: PlannedExpenseEntity) = a == b
    }
}
