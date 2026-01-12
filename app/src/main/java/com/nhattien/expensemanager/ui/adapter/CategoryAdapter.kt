package com.nhattien.expensemanager.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.domain.Category

class CategoryAdapter(
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var items: List<Category> = emptyList()
    private var selectedCategory: Category? = null

    fun submitList(newItems: List<Category>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelected(category: Category) {
        selectedCategory = category
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtIcon: TextView = itemView.findViewById(R.id.txtCategoryIcon)
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val root: View = itemView.findViewById(R.id.itemRoot)

        fun bind(item: Category) {
            txtName.text = item.label
            txtIcon.text = item.icon

            // Hiệu ứng khi được chọn
            if (item == selectedCategory) {
                root.setBackgroundResource(R.drawable.bg_today) // Tái sử dụng bg có viền
                txtName.setTextColor(Color.parseColor("#1976D2"))
            } else {
                root.setBackgroundResource(R.drawable.bg_calendar_day)
                txtName.setTextColor(Color.parseColor("#333333"))
            }

            itemView.setOnClickListener {
                selectedCategory = item
                notifyDataSetChanged()
                onCategoryClick(item)
            }
        }
    }
}
