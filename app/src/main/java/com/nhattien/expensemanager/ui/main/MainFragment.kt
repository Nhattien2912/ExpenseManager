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
import com.nhattien.expensemanager.domain.FilterType
import com.nhattien.expensemanager.domain.MainTab
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.Calendar
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import com.nhattien.expensemanager.utils.CurrencyUtils

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
        setupPieChart()
        setupBarChart()
        setupLineChart()
        setupRecyclerView()
        observeViewModel()
        
        // Register listener to sync spending limit
        viewModel.registerPrefsListener()
    }
    
    override fun onResume() {
        super.onResume()
        // Re-register listener when returning to fragment
        viewModel.registerPrefsListener()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.unregisterPrefsListener()
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
            
            // Apply Layout Animation
            val animation = android.view.animation.AnimationUtils.loadLayoutAnimation(context, R.anim.item_layout_animation)
            layoutAnimation = animation
        }

        // SWIPE TO DELETE
        val itemTouchHelperCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            
            // Drawing Resources making sure not to allocate in onChildDraw
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
            private val background = ColorDrawable(Color.parseColor("#F44336")) // Red

            override fun onMove(r: androidx.recyclerview.widget.RecyclerView, v: androidx.recyclerview.widget.RecyclerView.ViewHolder, t: androidx.recyclerview.widget.RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transactionWithCategory = adapter.currentList[position]
                viewModel.deleteTransaction(transactionWithCategory.transaction)
                
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
             viewModel.setTab(com.nhattien.expensemanager.domain.MainTab.REPORT)
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
        binding.root.findViewById<View>(R.id.btnFilterRecurring).setOnClickListener {
            viewModel.setFilter(FilterType.RECURRING)
            updateFilterUI(FilterType.RECURRING)
        }

        // Header Date Selection
        binding.btnSelectMonth.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, year, month, _ ->
                viewModel.setCurrentMonth(year, month)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // TAB LISTENERS
        binding.tabOverview.setOnClickListener { viewModel.setTab(com.nhattien.expensemanager.domain.MainTab.OVERVIEW) }
        binding.tabReport.setOnClickListener { viewModel.setTab(com.nhattien.expensemanager.domain.MainTab.REPORT) }
        
        // CHART SELECTION
        binding.chipGroupChartType.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.chipPie -> com.nhattien.expensemanager.domain.ChartType.PIE
                R.id.chipBar -> com.nhattien.expensemanager.domain.ChartType.BAR
                R.id.chipLine -> com.nhattien.expensemanager.domain.ChartType.LINE
                else -> com.nhattien.expensemanager.domain.ChartType.PIE
            }
            viewModel.setChartType(type)
        }
        
        // Set Limit Listener - both button and card
        binding.btnSetLimit.setOnClickListener { showSetLimitDialog() }
        binding.cardSpendingLimit.setOnClickListener { showSetLimitDialog() }
        
        // Day Selector Listeners
        binding.root.findViewById<View>(R.id.btnPrevDay)?.setOnClickListener {
            viewModel.setViewMode(com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.DAILY)
            val current = viewModel.selectedDate.value
            val newCal = Calendar.getInstance().apply { 
                timeInMillis = current.timeInMillis
                add(Calendar.DAY_OF_YEAR, -1)
            }
            viewModel.setSelectedDate(newCal)
        }
        
        binding.root.findViewById<View>(R.id.btnNextDay)?.setOnClickListener {
            viewModel.setViewMode(com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.DAILY)
            val current = viewModel.selectedDate.value
            val newCal = Calendar.getInstance().apply { 
                timeInMillis = current.timeInMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            viewModel.setSelectedDate(newCal)
        }
        
        // VIEW MONTH BUTTON
        binding.root.findViewById<View>(R.id.btnViewMonth)?.setOnClickListener {
            viewModel.setViewMode(com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.MONTHLY)
        }
        
        // Date Picker for Day (Material Style)
        binding.root.findViewById<View>(R.id.txtSelectedDay)?.setOnClickListener {
             viewModel.setViewMode(com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.DAILY)
             val current = viewModel.selectedDate.value.timeInMillis
             
             val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                 .setTitleText("Chọn ngày giao dịch")
                 .setSelection(current)
                 .setTheme(R.style.ThemeOverlay_App_DatePicker) 
                 .build()

             datePicker.addOnPositiveButtonClickListener { selection ->
                 // MaterialDatePicker returns UTC. Convert to Local.
                 val calendar = Calendar.getInstance()
                 calendar.timeInMillis = selection
                 // Fix Timezone offset issue (selection is UTC start of day)
                 // Or better: just set the fields.
                 // Actually selection is UTC. If I set timeInMillis directly to local calendar, 
                 // it might be wrong if "UTC 00:00" is "Yesterday Local" or "Today Local" depending on zone.
                 // Correct way:
                 val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                 utcCalendar.timeInMillis = selection
                 
                 val year = utcCalendar.get(Calendar.YEAR)
                 val month = utcCalendar.get(Calendar.MONTH)
                 val day = utcCalendar.get(Calendar.DAY_OF_MONTH)
                 
                 val newCal = Calendar.getInstance().apply {
                     set(year, month, day)
                 }
                 viewModel.setSelectedDate(newCal)
             }
             datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }
    
    private fun observeViewModel() {
        // ... Existing observers ...
        // Day Selector Observer & ViewMode
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(viewModel.selectedDate, viewModel.viewMode) { date, mode -> Pair(date, mode) }
            .collectLatest { (cal, mode) ->
                val txtDay = binding.root.findViewById<android.widget.TextView>(R.id.txtSelectedDay)
                val btnViewMonth = binding.root.findViewById<View>(R.id.btnViewMonth)
                
                if (mode == com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.MONTHLY) {
                    txtDay?.text = "Tất cả giao dịch tháng"
                    btnViewMonth?.visibility = View.GONE // Hide button when already in month mode
                } else {
                    // DAILY MODE
                    val now = Calendar.getInstance()
                    val isToday = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                                  now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                    
                    val sdf = java.text.SimpleDateFormat(if (isToday) "'Hôm nay', dd/MM/yyyy" else "dd/MM/yyyy", java.util.Locale.getDefault())
                    txtDay?.text = sdf.format(cal.time)
                    btnViewMonth?.visibility = View.VISIBLE // Show "View Month" option
                }
            }
        }
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
                binding.pieChart.visibility = if (type == com.nhattien.expensemanager.domain.ChartType.PIE) View.VISIBLE else View.GONE
                binding.barChart.visibility = if (type == com.nhattien.expensemanager.domain.ChartType.BAR) View.VISIBLE else View.GONE
                binding.lineChart.visibility = if (type == com.nhattien.expensemanager.domain.ChartType.LINE) View.VISIBLE else View.GONE
                
                // Animate when showing
                when (type) {
                    com.nhattien.expensemanager.domain.ChartType.PIE -> binding.pieChart.animateY(1000)
                    com.nhattien.expensemanager.domain.ChartType.BAR -> binding.barChart.animateY(1000)
                    com.nhattien.expensemanager.domain.ChartType.LINE -> binding.lineChart.animateX(1000)
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
                
                // Bind Overview Limit Card
                val limitStr = limit.toCurrency()
                val expenseStr = "Đã chi: ${expense.toCurrency()}"
                val remaining = limit - expense
                val remainingStr = "Còn: ${remaining.toCurrency()}"
                val color = if (remaining >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
                val progress = if (limit > 0) (expense / limit * 100).toInt() else 0

                val card = binding.root.findViewById<View>(R.id.cardlimitOverview) // Access included layout via ID
                if (card != null) {
                    card.findViewById<android.widget.TextView>(R.id.txtLimitAmount)?.text = limitStr
                    card.findViewById<android.widget.ProgressBar>(R.id.progressBarLimit)?.progress = progress.coerceIn(0, 100)
                    card.findViewById<android.widget.ProgressBar>(R.id.progressBarLimit)?.progressTintList = android.content.res.ColorStateList.valueOf(if (expense > limit) Color.RED else Color.parseColor("#2196F3"))
                    card.findViewById<android.widget.TextView>(R.id.txtSpentAmount)?.text = expenseStr
                    card.findViewById<android.widget.TextView>(R.id.txtRemainingAmount)?.apply {
                        text = remainingStr
                        setTextColor(color)
                    }
                    // Add click listener to Overview card
                    card.setOnClickListener { showSetLimitDialog() }
                }
            }
        }
    }

    private fun updateTabUI(tab: com.nhattien.expensemanager.domain.MainTab) {
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_active)
        val inactiveBg = null
        val activeTextColor = ContextCompat.getColor(requireContext(), R.color.text_white)
        val inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        if (tab == com.nhattien.expensemanager.domain.MainTab.OVERVIEW) {
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
        binding.progressBarReportLimit.progress = progress.coerceIn(0, 100)
        
        val progressColor = if (expense > limit) Color.RED else Color.parseColor("#2196F3")
        binding.progressBarReportLimit.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)

        binding.txtReportLimitAmount.text = limit.toCurrency()
        binding.txtReportSpentAmount.text = "Đã chi: ${expense.toCurrency()}"
        
        val remaining = limit - expense
        binding.txtReportRemainingAmount.text = "Còn lại: ${remaining.toCurrency()}"
        binding.txtReportRemainingAmount.setTextColor(if (remaining >= 0) Color.parseColor("#4CAF50") else Color.RED)
    }

    private fun showSetLimitDialog() {
        // Create styled dialog
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        
        val input = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Nhập hạn mức (VD: 5.000.000)"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
            // Set initial value formatted
            setText(CurrencyUtils.formatWithSeparator(viewModel.spendingLimit.value))
        }
        
        // Add MoneyTextWatcher for auto-formatting
        input.addTextChangedListener(CurrencyUtils.MoneyTextWatcher(input))
        
        container.addView(input)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("💰 Đặt hạn mức chi tiêu")
            .setMessage("Giới hạn tối đa: 999 tỷ đồng")
            .setView(container)
            .setPositiveButton("Lưu") { _, _ ->
                val amount = CurrencyUtils.parseFromSeparator(input.text.toString())
                viewModel.setSpendingLimit(amount)
                android.widget.Toast.makeText(context, "Đã đặt hạn mức: ${CurrencyUtils.formatWithSeparator(amount)} đ", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            
            // ADJUST HOLE RADIUS (Thin donut like image)
            setHoleRadius(65f)
            setTransparentCircleRadius(70f)
            
            // Get text color based on theme
            val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            val secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            
            setEntryLabelColor(textColor)
            setEntryLabelTextSize(11f)
            
            // LEGEND SETUP (Show Legend, Hide Chart Labels)
            setDrawEntryLabels(false) 
            legend.isEnabled = true 
            
            legend.apply {
                isEnabled = true
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                isWordWrapEnabled = true
                setDrawInside(false)
                this.textColor = secondaryColor
                textSize = 11f
                xEntrySpace = 20f 
            }
            
            setExtraOffsets(30f, 10f, 30f, 10f)

            // Drag deceleration
            dragDecelerationFrictionCoef = 0.95f
            
            // Animation
            animateY(1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        }
    }
    
    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            
            // Get colors based on theme
            val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            val secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            val gridColor = ContextCompat.getColor(requireContext(), R.color.divider)
            
            // X Axis styling
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                this.textColor = secondaryColor
                textSize = 10f
                axisLineColor = gridColor
            }
            
            // Y Axis Left styling
            axisLeft.apply {
                setDrawGridLines(true)
                this.gridColor = gridColor
                this.textColor = secondaryColor
                textSize = 10f
                axisMinimum = 0f
                axisLineColor = gridColor
                enableGridDashedLine(10f, 5f, 0f)
            }
            
            // Hide Right Y Axis
            axisRight.isEnabled = false
            
            // Legend
            legend.apply {
                isEnabled = true
                this.textColor = textColor
                textSize = 11f
            }
            
            // Apply custom rounded renderer
            val customRenderer = com.nhattien.expensemanager.ui.chart.RoundedBarChartRenderer(
                this, animator, viewPortHandler
            )
            customRenderer.setCornerRadius(16f)
            renderer = customRenderer
            
            // Marker/Tooltip
            val marker = com.nhattien.expensemanager.ui.chart.ChartMarkerView(requireContext(), R.layout.marker_view)
            marker.chartView = this
            this.marker = marker
            
            // Animation
            animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutCubic)
        }
    }
    
    private fun setupLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            
            // Get colors based on theme
            val textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            val secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            val gridColor = ContextCompat.getColor(requireContext(), R.color.divider)
            
            // X Axis styling
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                this.textColor = secondaryColor
                textSize = 10f
                axisLineColor = gridColor
            }
            
            // Y Axis Left styling
            axisLeft.apply {
                setDrawGridLines(true)
                this.gridColor = gridColor
                this.textColor = secondaryColor
                textSize = 10f
                axisLineColor = gridColor
                enableGridDashedLine(10f, 5f, 0f)
            }
            
            // Hide Right Y Axis
            axisRight.isEnabled = false
            
            // Legend
            legend.apply {
                isEnabled = true
                this.textColor = textColor
                textSize = 11f
            }
            
            // Marker/Tooltip
            val marker = com.nhattien.expensemanager.ui.chart.ChartMarkerView(requireContext(), R.layout.marker_view)
            marker.chartView = this
            this.marker = marker
            
            // Animation
            animateX(1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        }
    }

    private fun drawPieChart(distribution: Map<com.nhattien.expensemanager.data.entity.CategoryEntity, Double>) {
        val entries = distribution.map { (category, percentage) ->
            com.github.mikephil.charting.data.PieEntry(percentage.toFloat(), category.name)
        }

        val dataSet = com.github.mikephil.charting.data.PieDataSet(entries, "")
        // PASTEL COLORS (Soft Pink, Blue, Green, Orange)
        dataSet.colors = listOf(
            Color.parseColor("#90CAF9"), // Light Blue
            Color.parseColor("#F48FB1"), // Pink
            Color.parseColor("#A5D6A7"), // Green
            Color.parseColor("#FFCC80"), // Orange
            Color.parseColor("#CE93D8"), // Purple
            Color.parseColor("#80CBC4")  // Teal
        )
        
        // SLICE STYLING
        dataSet.sliceSpace = 3f // Space between slices
        dataSet.selectionShift = 5f
        
        // VALUES & LABELS OUTSIDE (Polyline)
        dataSet.yValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.OUTSIDE_SLICE
        dataSet.xValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.OUTSIDE_SLICE
        
        // Connecting Line Config
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.4f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.valueLineWidth = 1f
        dataSet.valueLineColor = Color.rgb(200, 200, 200) // Light Gray
        
        // Text Colors
        dataSet.valueTextColor = Color.DKGRAY
        dataSet.valueTextSize = 11f
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
        if (dailyMap.isEmpty()) return
        
        val entries = dailyMap.map { (day, amount) ->
            com.github.mikephil.charting.data.BarEntry(day.toFloat(), amount.toFloat())
        }.sortedBy { it.x }
        
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        
        val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Chi tiêu theo ngày")
        dataSet.apply {
            // Gradient colors will be applied by custom renderer
            color = Color.parseColor("#FF6B6B")
            valueTextColor = textColor
            valueTextSize = 9f
            setDrawValues(true)
            
            // Value formatter
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value < 1000) return ""
                    return com.nhattien.expensemanager.utils.CurrencyUtils.formatShort(value.toDouble())
                }
            }
        }
        
        val data = com.github.mikephil.charting.data.BarData(dataSet)
        data.barWidth = 0.7f
        
        binding.barChart.apply {
            this.data = data
            setFitBars(true)
            animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutBack)
            invalidate()
        }
    }
    
    private fun drawLineChart(points: List<Pair<Int, Double>>) {
        if (points.isEmpty()) return
        
        val entries = points.map { (day, amount) ->
            com.github.mikephil.charting.data.Entry(day.toFloat(), amount.toFloat())
        }
        
        val textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        
        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "Xu hướng tài sản")
        dataSet.apply {
            color = primaryColor
            setCircleColor(primaryColor)
            lineWidth = 2.5f
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleRadius = 2f
            circleHoleColor = Color.WHITE
            valueTextSize = 9f
            valueTextColor = textColor
            mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            
            // Gradient fill under the line
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_chart_fill)
            
            // Highlight
            highLightColor = primaryColor
            highlightLineWidth = 1f
            setDrawHorizontalHighlightIndicator(false)
            
            // Value formatter
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return ""
                }
            }
        }
        
        val data = com.github.mikephil.charting.data.LineData(dataSet)
        
        binding.lineChart.apply {
            this.data = data
            animateX(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
            invalidate()
        }
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
        val btnRecurring = binding.root.findViewById<android.widget.TextView>(R.id.btnFilterRecurring)

        // Reset all
        listOf(btnAll, btnIncome, btnExpense, btnRecurring).forEach {
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
            FilterType.RECURRING -> {
                btnRecurring.setTextColor(colorActive)
                btnRecurring.setBackgroundResource(bgActive)
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
