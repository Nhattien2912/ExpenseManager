package com.nhattien.expensemanager.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.nhattien.expensemanager.databinding.FragmentCalendarBinding
import com.nhattien.expensemanager.ui.adapter.CalendarAdapter
import com.nhattien.expensemanager.ui.daydetail.DayDetailFragment
import com.nhattien.expensemanager.utils.DateUtils
import com.nhattien.expensemanager.viewmodel.MainViewModel
import com.nhattien.expensemanager.domain.DailySum
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import com.nhattien.expensemanager.data.entity.TransactionWithCategory

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by activityViewModels()
    private val calendar = Calendar.getInstance()
    private lateinit var adapter: CalendarAdapter
    private lateinit var detailsAdapter: com.nhattien.expensemanager.ui.adapter.TransactionAdapter
    private var currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        setupListeners()
        
        // Mặc định chọn ngày hôm nay
        updateSelectedDayInfo(currentDay, DailySum())
    }

    private fun setupRecyclerView() {
        adapter = CalendarAdapter { dayUi ->
            if (dayUi.day > 0) {
                currentDay = dayUi.day
                // Cập nhật thông tin nhanh ở dưới
                updateSelectedDayInfo(dayUi.day, DailySum(dayUi.income, dayUi.expense))
            }
        }
        binding.rvCalendar.layoutManager = GridLayoutManager(context, 7)
        binding.rvCalendar.adapter = adapter
        
        // Detail Adapter
        detailsAdapter = com.nhattien.expensemanager.ui.adapter.TransactionAdapter {
            // Click to Edit (Optional: Navigate to Edit Activity)
            val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java).apply {
                putExtra("EXTRA_ID", it.id)
            }
            startActivity(intent)
        }
        binding.rvDayDetails.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.rvDayDetails.adapter = detailsAdapter

        // Swipe to Delete (Copied from MainFragment)
        // Enabling BOTH Left and Right swipe for better usability
        val itemTouchHelperCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT or androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            private val deleteIcon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
            private val background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#F44336"))

            override fun onMove(r: androidx.recyclerview.widget.RecyclerView, v: androidx.recyclerview.widget.RecyclerView.ViewHolder, t: androidx.recyclerview.widget.RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transactionWithCategory = detailsAdapter.currentList[position]
                viewModel.deleteTransaction(transactionWithCategory.transaction)
                
                com.google.android.material.snackbar.Snackbar.make(binding.root, "Đã xóa giao dịch", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction("Hoàn tác") {
                        // Undo logic if needed
                    }.show()
            }

            override fun onChildDraw(c: android.graphics.Canvas, recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2
                
                if (dX < 0) {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)
                    val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
                    val iconBottom = iconTop + deleteIcon.intrinsicHeight
                    val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon.setTint(android.graphics.Color.WHITE)
                    deleteIcon.draw(c)
                } else {
                    background.setBounds(0, 0, 0, 0)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvDayDetails)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.calendarDailyTotals.collectLatest { totals ->
                updateCalendarDisplay(totals)
                // Refresh detail list if data changes (and we have totals likely means we have data)
                refreshDetailList()
            }
        }
    }

    private fun setupListeners() {
        binding.btnPrevMonth.setOnClickListener {
            viewModel.changeMonth(-1)
            calendar.add(Calendar.MONTH, -1)
        }
        binding.btnNextMonth.setOnClickListener {
            viewModel.changeMonth(1)
            calendar.add(Calendar.MONTH, 1)
        }
    }

    private fun updateCalendarDisplay(dailyTotals: Map<Int, DailySum>) {
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        binding.txtMonthTitle.text = "Tháng $month / $year"

        val days = DateUtils.getDaysInMonth(month, year, dailyTotals)
        adapter.submitList(days)
        
        // Also update the summary for the currently selected day if it exists in the new data
        val sum = dailyTotals[currentDay] ?: DailySum()
        updateSelectedDayInfo(currentDay, sum)
    }

    private fun updateSelectedDayInfo(day: Int, sum: DailySum) {
        binding.txtSelectedDayInfo.text = "Ngày $day tháng ${calendar.get(Calendar.MONTH) + 1}"
        
        val net = sum.income - sum.expense
        binding.txtDayNet.text = "Tổng: ${if (net >= 0) "+" else ""}${com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(net)}"
        binding.txtDayNet.setTextColor(if (net >= 0) 0xFF2196F3.toInt() else 0xFFF44336.toInt())

        binding.txtDayIncome.text = "Thu: +${com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(sum.income)}"
        binding.txtDayExpense.text = "Chi: -${com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(sum.expense)}"
        
        binding.txtDayIncome.visibility = if (sum.income > 0) View.VISIBLE else View.INVISIBLE
        binding.txtDayExpense.visibility = if (sum.expense > 0) View.VISIBLE else View.INVISIBLE

        refreshDetailList()
    }
    
    private fun refreshDetailList() {
        // Filter transactions for the selected day
        val selectedMonth = calendar.get(Calendar.MONTH)
        val selectedYear = calendar.get(Calendar.YEAR)
        
        val all = viewModel.allTransactions.value // Assuming this is exposed or accessible. 
        // If not, we might need to expose it or filter from the list used to compute totals.
        // Actually MainViewModel has `allTransactions`.
        
        val filtered = all.filter { 
            val c = Calendar.getInstance().apply { timeInMillis = it.transaction.date }
            c.get(Calendar.DAY_OF_MONTH) == currentDay && 
            c.get(Calendar.MONTH) == selectedMonth && 
            c.get(Calendar.YEAR) == selectedYear
        }.sortedByDescending { it.transaction.date }
        
        detailsAdapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
