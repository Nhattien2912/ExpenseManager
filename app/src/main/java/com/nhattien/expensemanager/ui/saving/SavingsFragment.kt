package com.nhattien.expensemanager.ui.saving

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.adapter.TransactionAdapter
import com.nhattien.expensemanager.viewmodel.SavingsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class SavingsFragment : Fragment() {

    private val viewModel: SavingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_savings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtTotal = view.findViewById<TextView>(R.id.txtTotalSavings)
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvSavingsHistory)
        val btnAdd = view.findViewById<View>(R.id.btnAddSaving)
        val btnBack = view.findViewById<View>(R.id.btnBack)

        // Setup RecyclerView
        // Reusing TransactionAdapter since structure is compatible
        val adapter = TransactionAdapter { transaction ->
             // Edit logic if needed, or just view
             val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
             intent.putExtra("EXTRA_ID", transaction.id)
             startActivity(intent)
        }
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter

        // Observe Data
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalSavings.collectLatest { balance ->
                txtTotal.text = com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(balance)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.savingTransactions.collectLatest { list ->
                adapter.submitList(list)
            }
        }
        
        // Listeners
        btnAdd.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
            // Optional: Pass extra to pre-select 'Saving' category if possible, but AddActivity might not support it yet.
            startActivity(intent)
        }
        
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
