package com.nhattien.expensemanager.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.databinding.FragmentMainBinding
import com.nhattien.expensemanager.ui.adapter.TransactionAdapter
import com.nhattien.expensemanager.viewmodel.MainViewModel
import com.nhattien.expensemanager.viewmodel.FilterType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.Calendar
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by activityViewModels()
    private var isBalanceVisible = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupShortcuts()
        setupListeners()
        setupPieChart() // Add this
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupShortcuts() {
        with(binding) {
            // Debt
            btnShortcutDebt.txtTitle.text = "Sổ nợ"
            btnShortcutDebt.txtIcon.text = "📒"
            
            // Savings
            btnShortcutSavings.txtTitle.text = "Tiết kiệm"
            btnShortcutSavings.txtIcon.text = "🐷"
            btnShortcutSavings.root.setOnClickListener {
                (activity as? com.nhattien.expensemanager.ui.main.MainActivity)?.loadFragment(com.nhattien.expensemanager.ui.saving.SavingsFragment())
            }
            
            // Budget
            btnShortcutBudget.txtTitle.text = "Ngân sách"
            btnShortcutBudget.txtIcon.text = "📊"
        }
    }

    private fun setupRecyclerView() {
        val adapter = TransactionAdapter { transaction ->
            // EDIT: Mở màn hình AddTransaction với ID
            val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
            intent.putExtra("EXTRA_ID", transaction.id)
            startActivity(intent)
        }

        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        // SWIPE TO DELETE
        val itemTouchHelperCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            
            // Drawing Resources making sure not to allocate in onChildDraw
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
            private val background = ColorDrawable(Color.parseColor("#F44336")) // Red

            override fun onMove(r: androidx.recyclerview.widget.RecyclerView, v: androidx.recyclerview.widget.RecyclerView.ViewHolder, t: androidx.recyclerview.widget.RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = adapter.currentList[position]
                viewModel.deleteTransaction(transaction)
                
                com.google.android.material.snackbar.Snackbar.make(binding.root, "Đã xóa giao dịch", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction("Hoàn tác") {
                        // Undo logic implementation
                    }.show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - deleteIcon!!.intrinsicHeight) / 2
                
                if (dX < 0) { // Swiping Left
                    // Draw Red Background
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    // Draw Trash Icon
                    val iconTop = itemView.top + (itemView.height - deleteIcon.intrinsicHeight) / 2
                    val iconBottom = iconTop + deleteIcon.intrinsicHeight
                    val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    deleteIcon.setTint(Color.WHITE)
                    deleteIcon.draw(c)
                } else {
                    background.setBounds(0, 0, 0, 0)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvRecentTransactions)
    }



    private fun setupListeners() {
        binding.imgEye.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            binding.imgEye.setImageResource(
                if (isBalanceVisible) android.R.drawable.ic_menu_view 
                else android.R.drawable.ic_menu_close_clear_cancel
            )
            updateBalanceDisplay(viewModel.totalBalance.value)
        }

        binding.btnShortcutDebt.root.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(com.nhattien.expensemanager.ui.debt.DebtFragment())
        }
        
        binding.btnShortcutBudget.root.setOnClickListener {
             // Already in Main, maybe scroll to Report or remove? Keeping for legacy specific navigation if needed
             // For now, switch to Report Tab
             viewModel.setTab(com.nhattien.expensemanager.viewmodel.MainTab.REPORT)
        }
        
        // Handle "See All"
        binding.root.findViewById<View>(R.id.txtSeeAll)?.setOnClickListener {
             // Navigate to history
        }

        // Filter Listeners
        binding.root.findViewById<View>(R.id.btnFilterAll).setOnClickListener {
            viewModel.setFilter(FilterType.ALL)
            updateFilterUI(FilterType.ALL)
        }
        binding.root.findViewById<View>(R.id.btnFilterIncome).setOnClickListener {
            viewModel.setFilter(FilterType.INCOME)
            updateFilterUI(FilterType.INCOME)
        }
        binding.root.findViewById<View>(R.id.btnFilterExpense).setOnClickListener {
            viewModel.setFilter(FilterType.EXPENSE)
            updateFilterUI(FilterType.EXPENSE)
        }

        // Header Date Selection
        binding.btnSelectMonth.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, year, month, _ ->
                viewModel.setCurrentMonth(year, month)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // TAB LISTENERS
        binding.tabOverview.setOnClickListener { viewModel.setTab(com.nhattien.expensemanager.viewmodel.MainTab.OVERVIEW) }
        binding.tabReport.setOnClickListener { viewModel.setTab(com.nhattien.expensemanager.viewmodel.MainTab.REPORT) }
        
        // CHART SELECTION
        binding.chipGroupChartType.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.chipPie -> com.nhattien.expensemanager.viewmodel.ChartType.PIE
                R.id.chipBar -> com.nhattien.expensemanager.viewmodel.ChartType.BAR
                R.id.chipLine -> com.nhattien.expensemanager.viewmodel.ChartType.LINE
                else -> com.nhattien.expensemanager.viewmodel.ChartType.PIE
            }
            viewModel.setChartType(type)
        }
        
        // Set Limit Listener
        binding.btnSetLimit.setOnClickListener { showSetLimitDialog() }
    }

    private fun observeViewModel() {
        // ... Existing observers ...
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalBalance.collectLatest { balance -> updateBalanceDisplay(balance) }
        }
        
        // MONTHLY BALANCE (For Dashboard & Report Card)
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(viewModel.monthlyIncome, viewModel.monthlyExpense) { inc, exp -> Pair(inc, exp) }
            .collectLatest { (income, expense) ->
                // Dashboard
                val diff = income - expense
                binding.txtMonthBalance.apply {
                    text = diff.toCurrency()
                    setTextColor(if (diff >= 0) 0xFF69F0AE.toInt() else 0xFFFF8A80.toInt())
                }
                
                // Report Card
                binding.txtReportIncome.text = income.toCurrency()
                binding.txtReportExpense.text = expense.toCurrency()
                binding.txtReportBalance.apply {
                    text = diff.toCurrency()
                    setTextColor(if (diff >= 0) 0xFF2196F3.toInt() else 0xFFFF8A80.toInt())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentTransactions.collectLatest { list ->
                (binding.rvRecentTransactions.adapter as? TransactionAdapter)?.submitList(list)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todayBalance.collectLatest { balance ->
                binding.txtTodayBalance.apply {
                    text = balance.toCurrency()
                    setTextColor(if (balance >= 0) 0xFF69F0AE.toInt() else 0xFFFF8A80.toInt())
                }
            }
        }

        // TAB OBSERVER
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentTab.collectLatest { tab ->
                updateTabUI(tab)
            }
        }

        // CHART TYPE & DATA OBSERVER
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chartType.collectLatest { type ->
                binding.pieChart.visibility = if (type == com.nhattien.expensemanager.viewmodel.ChartType.PIE) View.VISIBLE else View.GONE
                binding.barChart.visibility = if (type == com.nhattien.expensemanager.viewmodel.ChartType.BAR) View.VISIBLE else View.GONE
                binding.lineChart.visibility = if (type == com.nhattien.expensemanager.viewmodel.ChartType.LINE) View.VISIBLE else View.GONE
                
                // Animate when showing
                when (type) {
                    com.nhattien.expensemanager.viewmodel.ChartType.PIE -> binding.pieChart.animateY(1000)
                    com.nhattien.expensemanager.viewmodel.ChartType.BAR -> binding.barChart.animateY(1000)
                    com.nhattien.expensemanager.viewmodel.ChartType.LINE -> binding.lineChart.animateX(1000)
                }
            }
        }

        // PIE CHART
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categoryDistribution.collectLatest { distribution ->
               if (distribution.isNotEmpty()) drawPieChart(distribution)
            }
        }
        
        // BAR CHART DATA
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailyExpenseData.collectLatest { map ->
                drawBarChart(map)
            }
        }
        
        // LINE CHART DATA
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.balanceTrendData.collectLatest { list ->
                drawLineChart(list)
            }
        }

        // SPENDING LIMIT
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                viewModel.spendingLimit,
                viewModel.currentMonthExpense
            ) { limit, expense -> Pair(limit, expense) }
            .collectLatest { (limit, expense) ->
                updateLimitUI(limit, expense)
            }
        }
    }

    private fun updateTabUI(tab: com.nhattien.expensemanager.viewmodel.MainTab) {
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_active)
        val inactiveBg = null
        val activeTextColor = ContextCompat.getColor(requireContext(), R.color.text_white)
        val inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        if (tab == com.nhattien.expensemanager.viewmodel.MainTab.OVERVIEW) {
            binding.layoutOverview.visibility = View.VISIBLE
            binding.layoutReport.visibility = View.GONE
            
            binding.tabOverview.background = activeBg
            binding.tabOverview.setTextColor(activeTextColor)
            binding.tabReport.background = inactiveBg
            binding.tabReport.setTextColor(inactiveTextColor)
        } else {
            binding.layoutOverview.visibility = View.GONE
            binding.layoutReport.visibility = View.VISIBLE
            
            binding.tabOverview.background = inactiveBg
            binding.tabOverview.setTextColor(inactiveTextColor)
            binding.tabReport.background = activeBg
            binding.tabReport.setTextColor(activeTextColor)
            
            // Re-animate chart when showing
            // binding.pieChart.animateY(1000) // Moved to chartType observer
        }
    }

    private fun updateLimitUI(limit: Double, expense: Double) {
        val progress = if (limit > 0) (expense / limit * 100).toInt() else 0
        binding.progressBarLimit.progress = progress.coerceIn(0, 100)
        
        val progressColor = if (expense > limit) Color.RED else Color.parseColor("#2196F3")
        binding.progressBarLimit.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)

        binding.txtLimitAmount.text = limit.toCurrency()
        binding.txtSpentAmount.text = "Đã chi: ${expense.toCurrency()}"
        
        val remaining = limit - expense
        binding.txtRemainingAmount.text = "Còn lại: ${remaining.toCurrency()}"
        binding.txtRemainingAmount.setTextColor(if (remaining >= 0) Color.parseColor("#4CAF50") else Color.RED)
    }

    private fun showSetLimitDialog() {
        // Simple Input Dialog
        val input = android.widget.EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(viewModel.spendingLimit.value.toLong().toString())

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Đặt hạn mức chi tiêu")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.setSpendingLimit(amount)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            
            // ADJUST HOLE RADIUS (Make slices thicker)
            setHoleRadius(45f)
            setTransparentCircleRadius(50f)
            
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(10f)
            
            // LEGEND SETUP (Keep as is)
            setDrawEntryLabels(false) 
            
            legend.apply {
                isEnabled = true
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                isWordWrapEnabled = true
                setDrawInside(false)
                textColor = Color.DKGRAY
                textSize = 11f
                xEntrySpace = 20f 
            }
            
            setExtraOffsets(10f, 10f, 10f, 10f)
        }
        
        // ... (Bar/Line setup remains)
    }

    private fun drawPieChart(distribution: Map<com.nhattien.expensemanager.domain.Category, Double>) {
        val entries = distribution.map { (category, percentage) ->
            com.github.mikephil.charting.data.PieEntry(percentage.toFloat(), category.label)
        }

        val dataSet = com.github.mikephil.charting.data.PieDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#EF5350"), Color.parseColor("#42A5F5"), 
            Color.parseColor("#66BB6A"), Color.parseColor("#FFA726"), 
            Color.parseColor("#AB47BC"), Color.parseColor("#26C6DA")
        )
        
        // VALUES INSIDE
        dataSet.yValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.INSIDE_SLICE
        dataSet.xValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.INSIDE_SLICE
        
        // Text Colors (White on colored slices)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 13f
        dataSet.valueTypeface = android.graphics.Typeface.DEFAULT_BOLD

        val data = com.github.mikephil.charting.data.PieData(dataSet)
        data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // Hide values smaller than 4% because slice is too small for text
                 if (value < 4f) return "" 
                return String.format("%.1f%%", value)
            }
        })
        
        binding.pieChart.data = data
        binding.pieChart.invalidate()
    }
    
    private fun drawBarChart(dailyMap: Map<Int, Double>) {
        val entries = dailyMap.map { (day, amount) ->
            com.github.mikephil.charting.data.BarEntry(day.toFloat(), amount.toFloat())
        }.sortedBy { it.x }
        
        val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Chi tiêu")
        dataSet.color = Color.parseColor("#F44336")
        dataSet.valueTextColor = Color.GRAY
        dataSet.valueTextSize = 10f
        
        val data = com.github.mikephil.charting.data.BarData(dataSet)
        data.barWidth = 0.6f
        
        binding.barChart.data = data
        binding.barChart.invalidate()
    }
    
    private fun drawLineChart(points: List<Pair<Int, Double>>) {
        val entries = points.map { (day, amount) ->
            com.github.mikephil.charting.data.Entry(day.toFloat(), amount.toFloat())
        }
        
        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "Tài sản")
        dataSet.color = Color.parseColor("#2196F3")
        dataSet.setCircleColor(Color.parseColor("#2196F3"))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 3f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = Color.GRAY
        dataSet.mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#BBDEFB") // Light Blue Fill
        dataSet.fillAlpha = 100
        
        val data = com.github.mikephil.charting.data.LineData(dataSet)
        binding.lineChart.data = data
        binding.lineChart.invalidate()
    }

    private fun updateFilterUI(type: FilterType) {
        val context = requireContext()
        val colorActive = ContextCompat.getColor(context, android.R.color.white)
        val colorInactive = ContextCompat.getColor(context, R.color.text_primary)
        val bgActive = R.drawable.bg_filter_active
        val bgInactive = R.drawable.bg_calendar_day

        val btnAll = binding.root.findViewById<android.widget.TextView>(R.id.btnFilterAll)
        val btnIncome = binding.root.findViewById<android.widget.TextView>(R.id.btnFilterIncome)
        val btnExpense = binding.root.findViewById<android.widget.TextView>(R.id.btnFilterExpense)

        // Reset all
        listOf(btnAll, btnIncome, btnExpense).forEach {
            it.setTextColor(colorInactive)
            it.setBackgroundResource(bgInactive)
        }

        // Set Active
        when (type) {
            FilterType.ALL -> {
                btnAll.setTextColor(colorActive)
                btnAll.setBackgroundResource(bgActive)
            }
            FilterType.INCOME -> {
                btnIncome.setTextColor(colorActive)
                btnIncome.setBackgroundResource(bgActive)
            }
            FilterType.EXPENSE -> {
                btnExpense.setTextColor(colorActive)
                btnExpense.setBackgroundResource(bgActive)
            }
        }
    }

    private fun updateBalanceDisplay(balance: Double) {
        binding.txtTotalBalance.text = if (isBalanceVisible) balance.toCurrency() else "**********"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Extension function to make code cleaner
fun Double.toCurrency(): String {
    return com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(this)
}
