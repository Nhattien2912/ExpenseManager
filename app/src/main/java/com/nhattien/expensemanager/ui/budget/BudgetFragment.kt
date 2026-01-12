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
import com.nhattien.expensemanager.domain.Category
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
        val input = android.widget.EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(budgetViewModel.spendingLimit.value.toLong().toString())

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Đặt hạn mức chi tiêu")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: 0.0
                budgetViewModel.setSpendingLimit(amount)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun drawPieChart(distribution: Map<Category, Double>) {
        val entries = distribution.map { (category, percentage) ->
            PieEntry(percentage.toFloat(), category.label)
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f

        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.invalidate()
    }
    
    // Extension helper (duplicated from MainFragment for simplicity)
    private fun Double.toCurrency(): String {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("vi", "VN"))
        return format.format(this).replace("₫", "đ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
