package com.nhattien.expensemanager.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import com.nhattien.expensemanager.R
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import com.nhattien.expensemanager.databinding.ActivitySearchBinding
import com.nhattien.expensemanager.domain.TransactionType
import com.nhattien.expensemanager.ui.adapter.SearchHistoryAdapter
import com.nhattien.expensemanager.ui.adapter.TransactionAdapter
import com.nhattien.expensemanager.ui.add.AddTransactionActivity
import com.nhattien.expensemanager.utils.CurrencyUtils
import com.nhattien.expensemanager.viewmodel.SearchViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()
    
    private lateinit var historyAdapter: SearchHistoryAdapter
    private lateinit var resultsAdapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener { finish() }

        // Search Input
        binding.edtSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.saveCurrentSearch()
                hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }

        binding.edtSearch.addTextChangedListener { text ->
            viewModel.onQueryChanged(text.toString())
            binding.btnClear.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        binding.btnClear.setOnClickListener {
            binding.edtSearch.text.clear()
            showKeyboard()
        }

        // History Adapter
        historyAdapter = SearchHistoryAdapter(
            onItemClick = { item ->
                binding.edtSearch.setText(item.query)
                binding.edtSearch.setSelection(item.query.length)
                viewModel.saveCurrentSearch()
                hideKeyboard()
            },
            onDeleteClick = { item ->
                viewModel.deleteHistoryItem(item)
            }
        )
        binding.rvRecentSearches.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = historyAdapter
        }

        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }

        // Results Adapter
        resultsAdapter = TransactionAdapter(
            onItemClick = { transaction ->
                val intent = Intent(this, AddTransactionActivity::class.java).apply {
                    putExtra("EXTRA_ID", transaction.id)
                }
                startActivity(intent)
            }
        )
        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = resultsAdapter
        }

        // Filter Chips
        binding.chipDate.setOnClickListener { showDatePicker() }
        binding.chipType.setOnClickListener { showTypePicker() }
        binding.chipAmount.setOnClickListener { showAmountPicker() }
        
        // Auto focus search
        binding.edtSearch.requestFocus()
        // showKeyboard() // Optional, sometimes annoying
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Recent History
                launch {
                    viewModel.recentSearches.collectLatest { history ->
                        historyAdapter.submitList(history)
                        val showHistory = history.isNotEmpty() && binding.edtSearch.text.isNullOrEmpty()
                        binding.layoutRecentHistory.visibility = if (showHistory) View.VISIBLE else View.GONE
                        
                        // If showing history, hide results to avoid clutter
                        if (showHistory) {
                            binding.rvSearchResults.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.GONE
                        }
                    }
                }

                // Search Results
                launch {
                    viewModel.searchResults.collectLatest { results ->
                        resultsAdapter.submitList(results)
                        
                        // Visibility logic
                        val isQueryEmpty = binding.edtSearch.text.isNullOrEmpty()
                        // Only show results if we have query OR filters
                        // But ViewModel returns empty if query blank & date null
                        
                        val hasResults = results.isNotEmpty()
                        
                        if (isQueryEmpty && viewModel.dateRange.value == null) {
                            // Showing history mode (handled above)
                             binding.rvSearchResults.visibility = View.GONE
                             binding.layoutEmpty.visibility = View.GONE
                        } else {
                            binding.layoutRecentHistory.visibility = View.GONE // Hide history when searching
                            binding.rvSearchResults.visibility = if (hasResults) View.VISIBLE else View.GONE
                            binding.layoutEmpty.visibility = if (!hasResults) View.VISIBLE else View.GONE
                        }
                    }
                }

                // Filter States
                launch {
                    viewModel.dateRange.collectLatest { range ->
                        binding.chipDate.isChecked = range != null
                        if (range != null) {
                            val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
                            val start = formatter.format(Date(range.first))
                            val end = formatter.format(Date(range.second))
                            binding.chipDate.text = "$start - $end"
                        } else {
                            binding.chipDate.text = "Tất cả thời gian"
                        }
                    }
                }
                
                launch {
                    viewModel.filterType.collectLatest { type ->
                        binding.chipType.isChecked = type != null
                        binding.chipType.text = when (type) {
                            TransactionType.INCOME -> "Thu nhập"
                            TransactionType.EXPENSE -> "Chi tiêu"
                            else -> "Loại: Tất cả"
                        }
                    }
                }
                
                launch {
                    viewModel.amountRange.collectLatest { range ->
                        binding.chipAmount.isChecked = range != null
                        if (range != null) {
                            binding.chipAmount.text = "${CurrencyUtils.toCurrency(range.first)} - ${CurrencyUtils.toCurrency(range.second)}"
                        } else {
                            binding.chipAmount.text = "Số tiền: Tất cả"
                        }
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Chọn khoảng thời gian")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            viewModel.setDateRange(selection.first, selection.second)
        }
        picker.addOnNegativeButtonClickListener {
            viewModel.setDateRange(null, null)
        }
        picker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun showTypePicker() {
        val options = arrayOf("Tất cả", "Thu nhập", "Chi tiêu")
        android.app.AlertDialog.Builder(this)
            .setTitle("Chọn loại giao dịch")
            .setItems(options) { _, which ->
                val type = when (which) {
                    1 -> TransactionType.INCOME
                    2 -> TransactionType.EXPENSE
                    else -> null
                }
                viewModel.setFilterType(type)
            }
            .show()
    }

    private fun showAmountPicker() {
        // Simple dialog for now, or could implement a range slider
        // Simple dialog for now, or could implement a range slider
        // For MVP, lets just use a simple alert with 3 presets or "All"
        
        val options = arrayOf(
            "Tất cả",
            "< 100.000 đ",
            "100k - 500k",
            "500k - 2 triệu",
            "> 2 triệu"
        )
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Khoảng tiền")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.setAmountRange(null, null)
                    1 -> viewModel.setAmountRange(0.0, 100000.0)
                    2 -> viewModel.setAmountRange(100000.0, 500000.0)
                    3 -> viewModel.setAmountRange(500000.0, 2000000.0)
                    4 -> viewModel.setAmountRange(2000000.0, Double.MAX_VALUE)
                }
            }
            .show()
    }

    private fun showKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.edtSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.edtSearch.windowToken, 0)
    }
}
