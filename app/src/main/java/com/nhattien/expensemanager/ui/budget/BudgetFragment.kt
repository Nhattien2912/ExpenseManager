package com.nhattien.expensemanager.ui.budget

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.databinding.FragmentBudgetBinding
import com.nhattien.expensemanager.ui.adapter.CategoryBudgetLimitAdapter
import com.nhattien.expensemanager.utils.CurrencyUtils
import com.nhattien.expensemanager.viewmodel.BudgetViewModel
import com.nhattien.expensemanager.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val budgetViewModel: BudgetViewModel by activityViewModels()

    private lateinit var categoryLimitAdapter: CategoryBudgetLimitAdapter
    private var cachedExpenseCategories: List<CategoryEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPieChart()
        setupCategoryLimitList()
        observeCategoryDistribution()
        setupBudgetListeners()
        observeBudgetViewModel()
    }

    private fun setupPieChart() {
        // Nothing needed here anymore, CustomDonutChartView handles its own setup
    }

    private fun setupCategoryLimitList() {
        categoryLimitAdapter = CategoryBudgetLimitAdapter { item ->
            showCategoryLimitDialog(
                categoryId = item.categoryId,
                categoryLabel = "${item.categoryIcon} ${item.categoryName}",
                currentLimit = item.limit
            )
        }

        binding.rvCategoryLimits.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryLimitAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeCategoryDistribution() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categoryDistribution.collectLatest { distribution ->
                if (distribution.isNotEmpty()) {
                    drawPieChart(distribution)
                }
            }
        }
    }

    private fun observeBudgetViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                budgetViewModel.spendingLimit,
                budgetViewModel.currentMonthExpense
            ) { limit: Double, expense: Double -> Pair(limit, expense) }
                .collectLatest { (limit, expense) ->
                    updateBudgetUI(limit, expense)
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            budgetViewModel.categoryLimitItems.collectLatest { items ->
                categoryLimitAdapter.submitList(items)
                binding.txtCategoryLimitEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            budgetViewModel.expenseCategories.collectLatest { categories ->
                cachedExpenseCategories = categories
            }
        }
    }

    private fun updateBudgetUI(limit: Double, expense: Double) {
        val progress = if (limit > 0) (expense / limit * 100).toInt() else 0
        binding.progressBarLimit.progress = progress.coerceIn(0, 100)

        val progressColor = if (expense > limit && limit > 0) Color.RED else Color.parseColor("#2196F3")
        binding.progressBarLimit.progressTintList = ColorStateList.valueOf(progressColor)

        binding.txtLimitAmount.text = limit.toCurrency()
        binding.txtSpentAmount.text = "Đã chi: ${expense.toCurrency()}"

        val remaining = limit - expense
        binding.txtRemainingAmount.text = "Còn lại: ${remaining.toCurrency()}"
        binding.txtRemainingAmount.setTextColor(if (remaining >= 0) Color.parseColor("#4CAF50") else Color.RED)
    }

    private fun setupBudgetListeners() {
        binding.btnSetLimit.setOnClickListener {
            showSetLimitDialog()
        }

        binding.btnAddCategoryLimit.setOnClickListener {
            showCategoryPickerDialog()
        }
    }

    private fun showSetLimitDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Nhập hạn mức (VD: 5.000.000)"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setText(CurrencyUtils.formatWithSeparator(budgetViewModel.spendingLimit.value))
        }

        input.addTextChangedListener(CurrencyUtils.MoneyTextWatcher(input))
        container.addView(input)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Đặt hạn mức chi tiêu tổng")
            .setView(container)
            .setPositiveButton("Lưu") { _, _ ->
                val amount = CurrencyUtils.parseFromSeparator(input.text.toString())
                budgetViewModel.setSpendingLimit(amount)
                Toast.makeText(
                    context,
                    "Đã đặt hạn mức: ${CurrencyUtils.formatWithSeparator(amount)} đ",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showCategoryPickerDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val categories = if (cachedExpenseCategories.isNotEmpty()) {
                cachedExpenseCategories
            } else {
                budgetViewModel.getExpenseCategoriesNow()
            }

            if (categories.isEmpty()) {
                Toast.makeText(context, "Chưa có danh mục chi tiêu", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val labels = categories.map { "${it.icon} ${it.name}" }.toTypedArray()

            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Chọn danh mục cần đặt hạn mức")
                .setItems(labels) { _, which ->
                    val selected = categories[which]
                    val current = budgetViewModel.categoryLimits.value[selected.id] ?: 0.0
                    showCategoryLimitDialog(
                        categoryId = selected.id,
                        categoryLabel = labels[which],
                        currentLimit = current
                    )
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun showCategoryLimitDialog(categoryId: Long, categoryLabel: String, currentLimit: Double) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }

        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Nhập hạn mức tháng"
            textSize = 18f
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
            if (currentLimit > 0) {
                setText(CurrencyUtils.formatWithSeparator(currentLimit))
            }
        }

        input.addTextChangedListener(CurrencyUtils.MoneyTextWatcher(input))
        container.addView(input)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Hạn mức: $categoryLabel")
            .setView(container)
            .setPositiveButton("Lưu") { _, _ ->
                val amount = CurrencyUtils.parseFromSeparator(input.text.toString())
                budgetViewModel.setCategorySpendingLimit(categoryId, amount)
                if (amount > 0) {
                    Toast.makeText(
                        context,
                        "Đã lưu hạn mức: ${CurrencyUtils.formatWithSeparator(amount)} đ",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, "Đã xóa hạn mức danh mục", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Xóa hạn mức") { _, _ ->
                budgetViewModel.clearCategorySpendingLimit(categoryId)
                Toast.makeText(context, "Đã xóa hạn mức danh mục", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun drawPieChart(distribution: Map<CategoryEntity, Double>) {
        if (distribution.isEmpty()) { 
            binding.pieChart.setData(emptyList(), "0 đ")
            binding.pieChart.invalidate() 
            return 
        }

        val total = distribution.values.sum()
        if (total == 0.0) { 
            binding.pieChart.setData(emptyList(), "0 đ")
            binding.pieChart.invalidate()
            return 
        }

        val palette = listOf(
            Color.parseColor("#42A5F5"),  // Bright Blue (largest)
            Color.parseColor("#F06292"),  // Pink 
            Color.parseColor("#FFB74D"),  // Orange
            Color.parseColor("#26A69A"),  // Teal
            Color.parseColor("#BDBDBD"),  // Gray (smallest)
            Color.parseColor("#AB47BC"),  // Purple
            Color.parseColor("#FF7043"),  // Deep Orange
            Color.parseColor("#5C6BC0"),  // Indigo
            Color.parseColor("#66BB6A"),  // Green
            Color.parseColor("#78909C")   // Blue Gray
        )

        val sorted = distribution.entries.sortedByDescending { it.value }
        val chartData = ArrayList<com.nhattien.expensemanager.ui.chart.DonutChartData>()

        sorted.forEachIndexed { index, entry ->
            chartData.add(
                com.nhattien.expensemanager.ui.chart.DonutChartData(
                    name = entry.key.name,
                    value = entry.value.toFloat(),
                    color = palette[index % palette.size],
                    icon = entry.key.icon
                )
            )
        }

        val totalTextFormatted = total.toCurrency()
        binding.pieChart.setData(chartData, totalTextFormatted)
    }

    private fun Double.toCurrency(): String {
        return CurrencyUtils.toCurrency(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
