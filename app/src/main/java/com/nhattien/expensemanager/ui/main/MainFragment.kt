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
import com.nhattien.expensemanager.viewmodel.BudgetViewModel
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

    companion object {
        private const val ARG_OPEN_REPORT = "open_report"

        fun newInstance(openReport: Boolean = false): MainFragment {
            val fragment = MainFragment()
            val args = Bundle()
            args.putBoolean(ARG_OPEN_REPORT, openReport)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by activityViewModels()
    private val budgetViewModel: BudgetViewModel by activityViewModels()
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
        // Register listener to sync spending limit
        viewModel.registerPrefsListener()
        
        if (arguments?.getBoolean(ARG_OPEN_REPORT) == true) {
            // Use post to ensure viewmodel is ready or just set it
            binding.root.post {
                viewModel.setTab(com.nhattien.expensemanager.domain.MainTab.REPORT)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.registerPrefsListener()
    }
    
    override fun onPause() {
        super.onPause()
        viewModel.unregisterPrefsListener()
    }

    private fun setupShortcuts() {
        with(binding) {
            btnShortcutDebt.txtTitle.text = "Sổ nợ"
            btnShortcutDebt.txtIcon.text = "📒"

            btnShortcutSavings.txtTitle.text = "Tiết kiệm"
            btnShortcutSavings.txtIcon.text = "🐷"
            btnShortcutSavings.root.setOnClickListener {
                (activity as? MainActivity)?.loadFragment(com.nhattien.expensemanager.ui.saving.SavingsFragment())
            }

            btnShortcutBudget.txtTitle.text = "Hạn mức"
            btnShortcutBudget.txtIcon.text = "📊"
            btnShortcutBudget.root.setOnClickListener {
                (activity as? MainActivity)?.loadFragment(com.nhattien.expensemanager.ui.budget.BudgetFragment())
            }

            btnSetLimit.text = "⚙ Cài đặt"
            btnCategoryLimits.text = "+ Danh mục"
        }
    }

    private fun setupRecyclerView() {
        val adapter = TransactionAdapter { transaction ->
            val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
            intent.putExtra("EXTRA_ID", transaction.id)
            startActivity(intent)
        }

        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
            val animation = android.view.animation.AnimationUtils.loadLayoutAnimation(context, R.anim.item_layout_animation)
            layoutAnimation = animation
        }

        // SWIPE TO DELETE
        val itemTouchHelperCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)
            private val background = ColorDrawable(Color.parseColor("#F44336")) // Red

            override fun onMove(r: androidx.recyclerview.widget.RecyclerView, v: androidx.recyclerview.widget.RecyclerView.ViewHolder, t: androidx.recyclerview.widget.RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transactionWithCategory = adapter.currentList[position]
                viewModel.deleteTransaction(transactionWithCategory.transaction)
                com.google.android.material.snackbar.Snackbar.make(binding.root, "Đã xóa giao dịch", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                    .setAction("Hoàn tác") { /* Undo logic */ }.show()
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
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
                    deleteIcon.setTint(Color.WHITE)
                    deleteIcon.draw(c)
                } else {
                    background.setBounds(0, 0, 0, 0)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvRecentTransactions)
        
        // Setup Report Transactions
        val reportAdapter = TransactionAdapter { transaction ->
            val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
            intent.putExtra("EXTRA_ID", transaction.id)
            startActivity(intent)
        }
        binding.rvReportTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = reportAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.imgEye.setOnClickListener {
            isBalanceVisible = !isBalanceVisible
            binding.imgEye.setImageResource(if (isBalanceVisible) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_close_clear_cancel)
            updateBalanceDisplay(viewModel.totalBalance.value)
        }

        binding.btnShortcutDebt.root.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(com.nhattien.expensemanager.ui.debt.DebtFragment())
        }
        
        binding.btnShortcutBudget.root.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(com.nhattien.expensemanager.ui.budget.BudgetFragment())
        }
        
        binding.btnNotifications.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.notification.NotificationActivity::class.java))
        }
        
        binding.root.findViewById<View>(R.id.txtSeeAll)?.setOnClickListener { }

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
        binding.root.findViewById<View>(R.id.btnFilterTag).setOnClickListener { showTagFilterDialog() }

        binding.btnSelectMonth.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, year, month, _ ->
                viewModel.setCurrentMonth(year, month)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.tabOverview.setOnClickListener { viewModel.setTab(com.nhattien.expensemanager.domain.MainTab.OVERVIEW) }
        binding.tabReport.setOnClickListener { viewModel.setTab(com.nhattien.expensemanager.domain.MainTab.REPORT) }
        
        binding.chipGroupChartType.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.chipPie -> com.nhattien.expensemanager.domain.ChartType.PIE
                R.id.chipBar -> com.nhattien.expensemanager.domain.ChartType.BAR
                R.id.chipLine -> com.nhattien.expensemanager.domain.ChartType.LINE
                else -> com.nhattien.expensemanager.domain.ChartType.PIE
            }
            viewModel.setChartType(type)
        }
        
        binding.btnSetLimit.setOnClickListener { showSetLimitDialog() }
        binding.cardSpendingLimit.setOnClickListener { showSetLimitDialog() }
        binding.btnCategoryLimits.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(com.nhattien.expensemanager.ui.budget.BudgetFragment())
        }
        binding.root.findViewById<View>(R.id.cardlimitOverview)?.setOnClickListener { showSetLimitDialog() }
        binding.root.findViewById<View>(R.id.cardlimitOverview)
            ?.findViewById<View>(R.id.btnCategoryLimitDetails)
            ?.setOnClickListener {
                (activity as? MainActivity)?.loadFragment(com.nhattien.expensemanager.ui.budget.BudgetFragment())
            }
        
        binding.root.findViewById<View>(R.id.btnPrevDay)?.setOnClickListener {
            viewModel.setViewMode(com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.DAILY)
            val current = viewModel.selectedDate.value
            val newCal = Calendar.getInstance().apply { timeInMillis = current.timeInMillis; add(Calendar.DAY_OF_YEAR, -1) }
            viewModel.setSelectedDate(newCal)
        }
        
        binding.root.findViewById<View>(R.id.btnNextDay)?.setOnClickListener {
            viewModel.setViewMode(com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.DAILY)
            val current = viewModel.selectedDate.value
            val newCal = Calendar.getInstance().apply { timeInMillis = current.timeInMillis; add(Calendar.DAY_OF_YEAR, 1) }
            viewModel.setSelectedDate(newCal)
        }
        
        binding.root.findViewById<View>(R.id.btnViewMonth)?.setOnClickListener {
            viewModel.setViewMode(com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.MONTHLY)
        }
        
        binding.root.findViewById<View>(R.id.txtSelectedDay)?.setOnClickListener {
             viewModel.setViewMode(com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.DAILY)
             val current = viewModel.selectedDate.value.timeInMillis
             com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                 .setTitleText("Chọn ngày")
                 .setSelection(current)
                 .setTheme(R.style.ThemeOverlay_App_DatePicker) 
                 .build()
                 .apply {
                     addOnPositiveButtonClickListener { selection ->
                         val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                         utcCalendar.timeInMillis = selection
                         val newCal = Calendar.getInstance().apply {
                             set(utcCalendar.get(Calendar.YEAR), utcCalendar.get(Calendar.MONTH), utcCalendar.get(Calendar.DAY_OF_MONTH))
                         }
                         viewModel.setSelectedDate(newCal)
                     }
                 }.show(parentFragmentManager, "DATE_PICKER")
        }
        
        // WALLET SELECTOR LISTENER (NEW)
        binding.root.findViewById<View>(R.id.btnSelectWallet)?.setOnClickListener {
            showWalletSelectionDialog()
        }
    }
    
    private fun showWalletSelectionDialog() {
        val wallets = viewModel.allWallets.value
        if (wallets.isEmpty()) return // Should not happen with prepopulate

        // Use Simple Single Choice Dialog for speed
        
        val options = mutableListOf<String>()
        options.add("Tất cả ví")
        options.addAll(wallets.map { it.name })
        
        val icons = mutableListOf<Int>()
        // Icons not supported in standard AlertDialog items easily without adapter.
        
        var selectedIndex = -1
        val current = viewModel.walletFilter.value
        selectedIndex = if (current == null) 0 else wallets.indexOfFirst { it.id == current.id } + 1
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Chọn ví")
            .setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialog, which ->
                if (which == 0) {
                    viewModel.setWalletFilter(null)
                } else {
                    viewModel.setWalletFilter(wallets[which - 1])
                }
                dialog.dismiss()
            }
            .setPositiveButton("Quản lý ví") { _, _ ->
                // Navigate to Manage Activity
                startActivity(android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.wallet.ManageWalletsActivity::class.java))
            }
            .setNeutralButton("Hủy", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(viewModel.selectedDate, viewModel.viewMode) { date, mode -> Pair(date, mode) }
            .collectLatest { (cal, mode) ->
                val txtDay = binding.root.findViewById<android.widget.TextView>(R.id.txtSelectedDay)
                val btnViewMonth = binding.root.findViewById<View>(R.id.btnViewMonth)
                
                if (mode == com.nhattien.expensemanager.viewmodel.MainViewModel.ViewMode.MONTHLY) {
                    txtDay?.text = "Tất cả giao dịch tháng"
                    btnViewMonth?.visibility = View.GONE
                } else {
                    val now = Calendar.getInstance()
                    val isToday = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                    val sdf = java.text.SimpleDateFormat(if (isToday) "'Hôm nay', dd/MM/yyyy" else "dd/MM/yyyy", java.util.Locale.getDefault())
                    txtDay?.text = sdf.format(cal.time)
                    btnViewMonth?.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalBalance.collectLatest { balance -> updateBalanceDisplay(balance) }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(viewModel.monthlyIncome, viewModel.monthlyExpense) { inc, exp -> Pair(inc, exp) }
            .collectLatest { (income, expense) ->
                val diff = income - expense
                binding.txtMonthBalance.apply {
                    text = diff.toCurrency()
                    setTextColor(if (diff >= 0) 0xFF69F0AE.toInt() else 0xFFFF8A80.toInt())
                }
                binding.txtReportIncome.text = income.toCurrency()
                binding.txtReportExpense.text = expense.toCurrency()
                binding.txtReportBalance.apply {
                    text = diff.toCurrency()
                    // Use White or Light Red for visibility on dark gradient
                    setTextColor(if (diff >= 0) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#FFCCBC"))
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentTransactions.collectLatest { list ->
                (binding.rvRecentTransactions.adapter as? TransactionAdapter)?.submitList(list)
                (binding.rvReportTransactions.adapter as? TransactionAdapter)?.submitList(list)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tagFilter.collectLatest { tag -> updateTagFilterUI(tag) }
        }
        
        // WALLET FILTER OBSERVER (NEW)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.walletFilter.collectLatest { wallet ->
                val txtName = binding.root.findViewById<android.widget.TextView>(R.id.txtWalletName)
                if (wallet == null) {
                    txtName.text = "Tất cả ví"
                    // Optionally set default icon
                } else {
                    txtName.text = wallet.name
                    // Optionally set specific icon
                }
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
        
        viewLifecycleOwner.lifecycleScope.launch {
            com.nhattien.expensemanager.data.database.AppDatabase.getInstance(requireContext())
                .notificationDao().getUnreadCount().collectLatest { count ->
                    binding.viewNotificationBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentTab.collectLatest { tab -> updateTabUI(tab) }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chartType.collectLatest { type ->
                binding.pieChart.visibility = if (type == com.nhattien.expensemanager.domain.ChartType.PIE) View.VISIBLE else View.GONE
                binding.barChart.visibility = if (type == com.nhattien.expensemanager.domain.ChartType.BAR) View.VISIBLE else View.GONE
                binding.lineChart.visibility = if (type == com.nhattien.expensemanager.domain.ChartType.LINE) View.VISIBLE else View.GONE
                
                when (type) {
                    com.nhattien.expensemanager.domain.ChartType.PIE -> binding.pieChart.animateY(1000)
                    com.nhattien.expensemanager.domain.ChartType.BAR -> binding.barChart.animateY(1000)
                    com.nhattien.expensemanager.domain.ChartType.LINE -> binding.lineChart.animateX(1000)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categoryDistribution.collectLatest { distribution -> if (distribution.isNotEmpty()) drawPieChart(distribution) }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailyExpenseData.collectLatest { map -> drawBarChart(map) }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.balanceTrendData.collectLatest { list -> drawLineChart(list) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(viewModel.spendingLimit, viewModel.currentMonthExpense) { limit, expense -> Pair(limit, expense) }
            .collectLatest { (limit, expense) ->
                updateLimitUI(limit, expense)
                val limitStr = limit.toCurrency()
                val expenseStr = "Đã chi: ${expense.toCurrency()}"
                val remaining = limit - expense
                val remainingStr = "Còn: ${remaining.toCurrency()}"
                val color = if (remaining >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
                val progress = if (limit > 0) (expense / limit * 100).toInt() else 0

                val card = binding.root.findViewById<View>(R.id.cardlimitOverview)
                if (card != null) {
                    card.findViewById<android.widget.TextView>(R.id.txtLimitAmount)?.text = limitStr
                    card.findViewById<android.widget.ProgressBar>(R.id.progressBarLimit)?.progress = progress.coerceIn(0, 100)
                    card.findViewById<android.widget.ProgressBar>(R.id.progressBarLimit)?.progressTintList = android.content.res.ColorStateList.valueOf(if (expense > limit) Color.RED else Color.parseColor("#2196F3"))
                    card.findViewById<android.widget.TextView>(R.id.txtSpentAmount)?.text = expenseStr
                    card.findViewById<android.widget.TextView>(R.id.txtRemainingAmount)?.apply {
                        text = remainingStr
                        setTextColor(color)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            budgetViewModel.categoryLimitItems.collectLatest { items ->
                val card = binding.root.findViewById<View>(R.id.cardlimitOverview) ?: return@collectLatest
                val txtSummary = card.findViewById<android.widget.TextView>(R.id.txtCategoryLimitSummary) ?: return@collectLatest
                val btnDetails = card.findViewById<android.widget.TextView>(R.id.btnCategoryLimitDetails) ?: return@collectLatest

                if (items.isEmpty()) {
                    txtSummary.text = "Danh mục: chưa đặt hạn mức"
                    btnDetails.text = "+ Danh mục"
                } else {
                    val total = items.size
                    val exceeded = items.count { it.isExceeded }
                    val warning = items.count { !it.isExceeded && it.progressPercent >= 80 }
                    txtSummary.text = "Danh mục: $total | Vượt: $exceeded | Cảnh báo: $warning"
                    btnDetails.text = "Xem $total mục"
                }
            }
        }
    }
    
    // UI Helper extensions and methods...
    private fun Double.toCurrency(): String = CurrencyUtils.toCurrency(this)
    
    private fun updateBalanceDisplay(balance: Double) {
        binding.txtTotalBalance.text = if (isBalanceVisible) balance.toCurrency() else "****"
    }
    
    private fun updateFilterUI(type: FilterType) {
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_active)
        val inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_day)
        val activeColor = ContextCompat.getColor(requireContext(), R.color.text_white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        
        val views = mapOf(
            FilterType.ALL to binding.root.findViewById<android.widget.TextView>(R.id.btnFilterAll),
            FilterType.INCOME to binding.root.findViewById<android.widget.TextView>(R.id.btnFilterIncome),
            FilterType.EXPENSE to binding.root.findViewById<android.widget.TextView>(R.id.btnFilterExpense),
            FilterType.RECURRING to binding.root.findViewById<android.widget.TextView>(R.id.btnFilterRecurring)
        )
        
        views.forEach { (filterType, view) ->
             if (filterType == type) {
                 view.background = activeBg
                 view.setTextColor(activeColor)
             } else {
                 view.background = inactiveBg
                 view.setTextColor(inactiveColor)
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
        }
    }

    private fun updateLimitUI(limit: Double, expense: Double) {
        val progress = if (limit > 0) (expense / limit * 100).toInt() else 0
        binding.progressBarReportLimit.progress = progress.coerceIn(0, 100)
        binding.progressBarReportLimit.progressTintList = android.content.res.ColorStateList.valueOf(if (expense > limit) Color.RED else Color.parseColor("#2196F3"))
        binding.txtReportLimitAmount.text = limit.toCurrency()
        binding.txtReportSpentAmount.text = "Đã chi: ${expense.toCurrency()}"
        val remaining = limit - expense
        binding.txtReportRemainingAmount.text = "Còn lại: ${remaining.toCurrency()}"
        binding.txtReportRemainingAmount.setTextColor(if (remaining >= 0) Color.parseColor("#4CAF50") else Color.RED)
    }

    private fun showSetLimitDialog() {
        val container = android.widget.LinearLayout(requireContext()).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(48, 32, 48, 16) }
        val input = android.widget.EditText(requireContext()).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER; hint = "Nhập hạn mức"; setText(CurrencyUtils.formatWithSeparator(viewModel.spendingLimit.value)) }
        input.addTextChangedListener(CurrencyUtils.MoneyTextWatcher(input))
        container.addView(input)
        android.app.AlertDialog.Builder(requireContext()).setTitle("Đặt hạn mức").setView(container)
            .setPositiveButton("Lưu") { _, _ -> viewModel.setSpendingLimit(CurrencyUtils.parseFromSeparator(input.text.toString())) }
            .setNegativeButton("Hủy", null).show()
    }
    
    private fun showTagFilterDialog() {
        val tags = viewModel.allTagsList.value
        val tagNames = tags.map { it.name }.toTypedArray<CharSequence>()
        android.app.AlertDialog.Builder(requireContext()).setTitle("Lọc theo Tag").setItems(tagNames) { _, w -> viewModel.setTagFilter(tags[w]) }
            .setNegativeButton("Hủy", null).setNeutralButton("Xóa lọc") { _, _ -> viewModel.setTagFilter(null) }.show()
    }
    
    private fun updateTagFilterUI(tag: com.nhattien.expensemanager.data.entity.TagEntity?) {
        val btnTag = binding.root.findViewById<android.widget.TextView>(R.id.btnFilterTag)
        val chipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupTagFilter)
        if (tag != null) {
            btnTag.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_active)
            btnTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_white))
            chipGroup.visibility = View.VISIBLE
            chipGroup.removeAllViews()
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = "Tag: ${tag.name}"
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener { viewModel.setTagFilter(null) }
            chipGroup.addView(chip)
        } else {
            btnTag.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_day)
            btnTag.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            chipGroup.visibility = View.GONE
        }
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setHoleRadius(55f)
            setTransparentCircleRadius(58f)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(80)
            
            // ENABLE entry labels (category names) - drawn separately from values
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.parseColor("#757575")) // Gray for category name
            setEntryLabelTextSize(13f) // Category name size
            setEntryLabelTypeface(android.graphics.Typeface.DEFAULT) // Regular weight
            
            // No legend - outside labels act as legend
            legend.isEnabled = false
            
            // Extra offsets for outside labels
            setExtraOffsets(8f, 4f, 8f, 4f)
            
            isRotationEnabled = true
            rotationAngle = 0f
            isHighlightPerTapEnabled = true
            
            animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutCubic)
        }
    }
    
    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setFitBars(true)
            
            renderer = com.nhattien.expensemanager.ui.chart.RoundedBarChartRenderer(this, animator, viewPortHandler).apply { setCornerRadius(12f) }
            
            // X axis
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.parseColor("#757575")
                textSize = 11f
                axisLineColor = Color.parseColor("#E0E0E0")
            }
            
            // Left Y axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                gridLineWidth = 0.8f
                textColor = Color.parseColor("#757575")
                textSize = 10f
                axisMinimum = 0f
                axisLineColor = Color.TRANSPARENT
                enableGridDashedLine(8f, 4f, 0f)
            }
            
            axisRight.isEnabled = false
            
            val mv = com.nhattien.expensemanager.ui.chart.ChartMarkerView(requireContext(), R.layout.marker_view)
            mv.chartView = this
            marker = mv
            
            animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutCubic)
        }
    }
    
    private fun setupLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setScaleEnabled(false)
            setPinchZoom(false)
            
            // X axis
            xAxis.apply {
                position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.parseColor("#757575")
                textSize = 11f
                axisLineColor = Color.parseColor("#E0E0E0")
            }
            
            // Left Y axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#F0F0F0")
                gridLineWidth = 0.8f
                textColor = Color.parseColor("#757575")
                textSize = 10f
                axisLineColor = Color.TRANSPARENT
                enableGridDashedLine(8f, 4f, 0f)
            }
            
            axisRight.isEnabled = false
            
            legend.apply {
                isEnabled = true
                textSize = 11f
                textColor = Color.parseColor("#616161")
                form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
                formSize = 8f
            }
            
            val mv = com.nhattien.expensemanager.ui.chart.ChartMarkerView(requireContext(), R.layout.marker_view)
            mv.chartView = this
            marker = mv
            
            animateX(1200, com.github.mikephil.charting.animation.Easing.EaseInOutCubic)
        }
    }

    private fun drawPieChart(distribution: Map<com.nhattien.expensemanager.data.entity.CategoryEntity, Double>) {
        if (distribution.isEmpty()) { binding.pieChart.clear(); return }
        
        val total = distribution.values.sum()
        if (total == 0.0) { binding.pieChart.clear(); return }
        
        // Vibrant pastel palette
        val palette = listOf(
            Color.parseColor("#42A5F5"),  // Bright Blue
            Color.parseColor("#F06292"),  // Pink
            Color.parseColor("#FFB74D"),  // Orange
            Color.parseColor("#26A69A"),  // Teal
            Color.parseColor("#BDBDBD"),  // Gray (smallest)
            Color.parseColor("#AB47BC"),  // Purple
            Color.parseColor("#FF7043"),  // Deep Orange
            Color.parseColor("#5C6BC0"),  // Indigo
            Color.parseColor("#8D6E63"),  // Brown
            Color.parseColor("#78909C")   // Blue Gray
        )
        
        // Sort by value descending and build entries
        val sorted = distribution.entries.sortedByDescending { it.value }
        val entries = ArrayList<com.github.mikephil.charting.data.PieEntry>()
        val colors = ArrayList<Int>()
        
        sorted.forEachIndexed { index, entry ->
            val percent = (entry.value / total * 100).toInt()
            val icon = entry.key.icon
            // Label = category name (shown by setDrawEntryLabels)
            entries.add(com.github.mikephil.charting.data.PieEntry(
                entry.value.toFloat(),
                entry.key.name,
                entry.key
            ))
            colors.add(palette[index % palette.size])
        }
        
        val dataSet = com.github.mikephil.charting.data.PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
            selectionShift = 6f
            
            // Values (percentage) go OUTSIDE
            yValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.OUTSIDE_SLICE
            // Entry labels (category name) also OUTSIDE
            xValuePosition = com.github.mikephil.charting.data.PieDataSet.ValuePosition.OUTSIDE_SLICE
            
            // Connector lines: short so labels stay close
            valueLinePart1OffsetPercentage = 85f
            valueLinePart1Length = 0.2f
            valueLinePart2Length = 0.2f
            valueLineWidth = 1f
            valueLineColor = Color.parseColor("#CCCCCC")
            isUsingSliceColorAsValueLineColor = false
            
            // Value text: bold percentage like "🏠 20%"
            valueTextSize = 16f
            valueTextColor = Color.parseColor("#333333")
            valueTypeface = android.graphics.Typeface.DEFAULT_BOLD
            
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val pct = (value / total * 100).toInt()
                    return "$pct%"
                }
            }
        }
        
        binding.pieChart.apply {
            data = com.github.mikephil.charting.data.PieData(dataSet)
            highlightValues(null)
            invalidate()
            animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        }
    }
    
    private fun drawBarChart(map: Map<Int, Double>) {
        val entries = map.entries.sortedBy { it.key }.map { com.github.mikephil.charting.data.BarEntry(it.key.toFloat(), it.value.toFloat()) }
        if (entries.isEmpty()) { binding.barChart.clear(); return }
        
        // Gradient-like multi-color bars
        val barColors = entries.map { entry ->
            val ratio = if (entries.maxOf { it.y } > 0) entry.y / entries.maxOf { it.y } else 0f
            when {
                ratio > 0.7f -> Color.parseColor("#FF5252")
                ratio > 0.4f -> Color.parseColor("#FFB347")
                else -> Color.parseColor("#5B9BD5")
            }
        }
        
        val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Chi tiêu theo ngày").apply {
            colors = barColors
            valueTextColor = Color.parseColor("#616161")
            valueTextSize = 9f
            setDrawValues(true)
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value >= 1000000) String.format("%.1fM", value / 1000000)
                    else if (value >= 1000) String.format("%.0fK", value / 1000)
                    else String.format("%.0f", value)
                }
            }
        }
        
        binding.barChart.data = com.github.mikephil.charting.data.BarData(dataSet).apply { barWidth = 0.5f }
        binding.barChart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseInOutCubic)
        binding.barChart.invalidate()
    }
    
    private fun drawLineChart(list: List<Pair<Int, Double>>) {
        if (list.isEmpty()) { binding.lineChart.clear(); return }
        val entries = list.map { com.github.mikephil.charting.data.Entry(it.first.toFloat(), it.second.toFloat()) }
        
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "Số dư").apply {
            color = primaryColor
            lineWidth = 2.5f
            setCircleColor(primaryColor)
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleRadius = 2f
            circleHoleColor = Color.WHITE
            mode = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.15f
            setDrawFilled(true)
            fillColor = primaryColor
            fillAlpha = 40
            setDrawValues(false)
            setDrawHighlightIndicators(true)
            highLightColor = Color.parseColor("#80" + Integer.toHexString(primaryColor).substring(2))
        }
        
        binding.lineChart.data = com.github.mikephil.charting.data.LineData(dataSet)
        binding.lineChart.animateX(1000, com.github.mikephil.charting.animation.Easing.EaseInOutCubic)
        binding.lineChart.invalidate()
    }
}
