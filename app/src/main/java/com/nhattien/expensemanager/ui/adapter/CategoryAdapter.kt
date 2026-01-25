package com.nhattien.expensemanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.domain.Category

import com.nhattien.expensemanager.data.entity.CategoryEntity

class CategoryAdapter(
    private val onCategoryClick: (CategoryEntity) -> Unit,
    private val onAddCategoryClick: () -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var items: List<CategoryEntity> = emptyList()
    private var selectedCategory: CategoryEntity? = null

    fun submitList(newItems: List<CategoryEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelected(category: CategoryEntity?) {
        selectedCategory = category
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        if (position == 0) {
            holder.bindAddButton()
        } else {
            holder.bind(items[position - 1])
        }
    }

    override fun getItemCount(): Int = items.size + 1

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtIcon: TextView = itemView.findViewById(R.id.txtCategoryIcon)
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val root: View = itemView.findViewById(R.id.itemRoot)

        fun bindAddButton() {
            txtName.text = "Thêm"
            txtIcon.text = "+" // Hoặc dùng icon drawable nếu muốn đẹp hơn
            txtIcon.textSize = 24f
            
            // Style riêng cho nút Add
            val context = itemView.context
            root.setBackgroundResource(R.drawable.bg_category_item)
            txtName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            txtIcon.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

            itemView.setOnClickListener {
                onAddCategoryClick()
            }
        }

        fun bind(item: CategoryEntity) {
            try {
                txtName.text = item.name
                txtIcon.text = item.icon
                txtIcon.textSize = 20f

                // Hiệu ứng khi được chọn
                val context = itemView.context
                val isSelected = selectedCategory?.id == item.id
                if (isSelected) {
                    root.setBackgroundResource(R.drawable.bg_today) // Tái sử dụng bg có viền
                    txtName.setTextColor(ContextCompat.getColor(context, R.color.primary))
                } else {
                    root.setBackgroundResource(R.drawable.bg_category_item)
                    txtName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }

                itemView.setOnClickListener {
                    selectedCategory = item
                    notifyDataSetChanged()
                    onCategoryClick(item)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                txtName.text = "Error"
            }
        }
    }
}
