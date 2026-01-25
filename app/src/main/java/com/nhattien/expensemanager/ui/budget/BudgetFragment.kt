package com.nhattien.expensemanager.ui.budget

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.databinding.FragmentBudgetBinding

import com.nhattien.expensemanager.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MainViewModel by activityViewModels()
    private val budgetViewModel: com.nhattien.expensemanager.viewmodel.BudgetViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupPieChart()
        observeCategoryDistribution()
        setupBudgetListeners()
        observeBudgetViewModel()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            legend.isEnabled = false
            animateY(1000)
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
            kotlinx.coroutines.flow.combine(
                budgetViewModel.spendingLimit,
                budgetViewModel.currentMonthExpense
            ) { limit: Double, expense: Double -> Pair(limit, expense) }
            .collectLatest { (limit, expense) ->
                updateBudgetUI(limit, expense)
            }
        }
    }

    private fun updateBudgetUI(limit: Double, expense: Double) {
        val progress = if (limit > 0) (expense / limit * 100).toInt() else 0
        binding.progressBarLimit.progress = progress.coerceIn(0, 100)
        
        // Color logic
        val progressColor = if (expense > limit) Color.RED else Color.parseColor("#2196F3")
        binding.progressBarLimit.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)

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
    }

    private fun showSetLimitDialog() {
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
            setText(com.nhattien.expensemanager.utils.CurrencyUtils.formatWithSeparator(budgetViewModel.spendingLimit.value))
        }
        
        input.addTextChangedListener(com.nhattien.expensemanager.utils.CurrencyUtils.MoneyTextWatcher(input))
        container.addView(input)

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("💰 Đặt hạn mức chi tiêu")
            .setMessage("Giới hạn tối đa: 999 tỷ đồng")
            .setView(container)
            .setPositiveButton("Lưu") { _, _ ->
                val amount = com.nhattien.expensemanager.utils.CurrencyUtils.parseFromSeparator(input.text.toString())
                budgetViewModel.setSpendingLimit(amount)
                android.widget.Toast.makeText(context, "Đã đặt hạn mức: ${com.nhattien.expensemanager.utils.CurrencyUtils.formatWithSeparator(amount)} đ", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun drawPieChart(distribution: Map<com.nhattien.expensemanager.data.entity.CategoryEntity, Double>) {
        val entries = distribution.map { (category, percentage) ->
            PieEntry(percentage.toFloat(), category.name)
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f

        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.invalidate()
    }
    
    // Extension helper - uses CurrencyUtils with MAX_AMOUNT limit
    private fun Double.toCurrency(): String {
        return com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
