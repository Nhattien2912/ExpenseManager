package com.nhattien.expensemanager.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.utils.CurrencyUtils
import com.nhattien.expensemanager.viewmodel.CategoryBudgetLimitItem

class CategoryBudgetLimitAdapter(
    private val onItemClick: (CategoryBudgetLimitItem) -> Unit
) : ListAdapter<CategoryBudgetLimitItem, CategoryBudgetLimitAdapter.CategoryBudgetLimitViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryBudgetLimitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_budget_limit, parent, false)
        return CategoryBudgetLimitViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryBudgetLimitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryBudgetLimitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtCategoryName: TextView = itemView.findViewById(R.id.txtCategoryName)
        private val txtAmountRatio: TextView = itemView.findViewById(R.id.txtAmountRatio)
        private val progressCategoryLimit: ProgressBar = itemView.findViewById(R.id.progressCategoryLimit)
        private val txtCategorySpent: TextView = itemView.findViewById(R.id.txtCategorySpent)
        private val txtCategoryRemaining: TextView = itemView.findViewById(R.id.txtCategoryRemaining)
        private val txtCategoryWarning: TextView = itemView.findViewById(R.id.txtCategoryWarning)

        fun bind(item: CategoryBudgetLimitItem) {
            txtCategoryName.text = "${item.categoryIcon} ${item.categoryName}"
            txtAmountRatio.text = "${CurrencyUtils.toCurrency(item.spent)} / ${CurrencyUtils.toCurrency(item.limit)}"

            val clampedProgress = item.progressPercent.coerceIn(0, 100)
            progressCategoryLimit.progress = clampedProgress

            val progressColor = if (item.isExceeded) {
                ContextCompat.getColor(itemView.context, R.color.expense)
            } else {
                ContextCompat.getColor(itemView.context, R.color.primary)
            }
            progressCategoryLimit.progressTintList = ColorStateList.valueOf(progressColor)

            txtCategorySpent.text = "Đã chi: ${CurrencyUtils.toCurrency(item.spent)}"

            if (item.remaining >= 0) {
                txtCategoryRemaining.text = "Còn: ${CurrencyUtils.toCurrency(item.remaining)}"
                txtCategoryRemaining.setTextColor(ContextCompat.getColor(itemView.context, R.color.income))
            } else {
                txtCategoryRemaining.text = "Vượt: ${CurrencyUtils.toCurrency(kotlin.math.abs(item.remaining))}"
                txtCategoryRemaining.setTextColor(ContextCompat.getColor(itemView.context, R.color.expense))
            }

            txtCategoryWarning.visibility = if (item.isExceeded) View.VISIBLE else View.GONE
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryBudgetLimitItem>() {
        override fun areItemsTheSame(oldItem: CategoryBudgetLimitItem, newItem: CategoryBudgetLimitItem): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }

        override fun areContentsTheSame(oldItem: CategoryBudgetLimitItem, newItem: CategoryBudgetLimitItem): Boolean {
            return oldItem == newItem
        }
    }
}
