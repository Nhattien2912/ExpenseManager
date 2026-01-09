package com.nhattien.expensemanager.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.model.CalendarDayUi
import com.nhattien.expensemanager.viewmodel.MainViewModel

class CalendarAdapter(
    private val onDayClick: (CalendarDayUi) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private var items: List<CalendarDayUi> = emptyList()

    fun submitList(newItems: List<CalendarDayUi>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val txtDay: TextView = itemView.findViewById(R.id.txtDay)
        private val txtTotal: TextView = itemView.findViewById(R.id.txtTotal)

        fun bind(item: CalendarDayUi) {
            txtDay.text = item.day.toString()
            txtTotal.text = formatMoney(item.total)
            if (item.day == 0) {
                itemView.visibility = View.INVISIBLE // Tàng hình
                return
            } else {
                itemView.visibility = View.VISIBLE // Hiện hình
            }

            // ... Code cũ giữ nguyên (gán text, màu sắc...)
            txtDay.text = item.day.toString()
            // màu âm / dương
            when {
                item.total < 0 -> txtTotal.setTextColor(0xFFD32F2F.toInt())
                item.total > 0 -> txtTotal.setTextColor(0xFF2E7D32.toInt())
                else -> txtTotal.setTextColor(0xFF999999.toInt())
            }

            // highlight hôm nay
            if (item.isToday) {
                itemView.setBackgroundResource(R.drawable.bg_today)
            } else {
                itemView.setBackgroundResource(R.drawable.bg_calendar_day)
            }

            itemView.setOnClickListener {
                onDayClick(item)
            }
        }

        private fun formatMoney(value: Double): String {
            return when {
                kotlin.math.abs(value) >= 1_000_000 -> "${(value / 1_000_000).toInt()}Tr"
                kotlin.math.abs(value) >= 1_000 -> "${(value / 1_000).toInt()}K"
                else -> value.toInt().toString()
            }
        }
    }
}
