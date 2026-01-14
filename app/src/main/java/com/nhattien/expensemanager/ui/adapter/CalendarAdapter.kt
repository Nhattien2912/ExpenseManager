package com.nhattien.expensemanager.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.model.CalendarDayUi

class CalendarAdapter(
    private val onDayClick: (CalendarDayUi) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var items: List<CalendarDayUi> = emptyList()
    private var selectedPosition: Int = -1

    fun submitList(newItems: List<CalendarDayUi>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDay: TextView = itemView.findViewById(R.id.txtDay)
        private val txtIncome: TextView = itemView.findViewById(R.id.txtIncome)
        private val txtExpense: TextView = itemView.findViewById(R.id.txtExpense)

        fun bind(item: CalendarDayUi, position: Int) {
            if (item.day <= 0) {
                itemView.visibility = View.INVISIBLE
                return
            }
            itemView.visibility = View.VISIBLE
            txtDay.text = item.day.toString()

            // Hiển thị Tiền Thu (+)
            if (item.income > 0) {
                txtIncome.visibility = View.VISIBLE
                val incomeText = com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(item.income)
                txtIncome.text = "+$incomeText"
            } else {
                txtIncome.visibility = View.GONE // Use GONE to save space
            }

            // Hiển thị Tiền Chi (-)
            if (item.expense > 0) {
                txtExpense.visibility = View.VISIBLE
                val expenseText = com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(item.expense)
                txtExpense.text = "-$expenseText"
            } else {
                txtExpense.visibility = View.GONE
            }

            // Hiệu ứng chọn ngày
            when {
                position == selectedPosition -> {
                    txtDay.setBackgroundResource(R.drawable.bg_today)
                    txtDay.setTextColor(Color.WHITE)
                }
                item.isToday -> {
                    txtDay.setBackgroundResource(R.drawable.bg_calendar_day)
                    txtDay.setTextColor(Color.parseColor("#2196F3")) 
                }
                else -> {
                    txtDay.setBackgroundResource(0)
                    txtDay.setTextColor(Color.parseColor("#333333"))
                }
            }

            itemView.setOnClickListener {
                val oldPos = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(oldPos)
                notifyItemChanged(selectedPosition)
                onDayClick(item)
            }
        }
    }
}
