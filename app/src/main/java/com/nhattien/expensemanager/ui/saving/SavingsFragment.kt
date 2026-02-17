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
import com.nhattien.expensemanager.ui.adapter.SavingsBucketAdapter
import com.nhattien.expensemanager.ui.adapter.TransactionAdapter
import com.nhattien.expensemanager.viewmodel.SavingsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SavingsFragment : Fragment() {

    private val viewModel: SavingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_savings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtTotal = view.findViewById<TextView>(R.id.txtTotalSavings)
        val rvBuckets = view.findViewById<RecyclerView>(R.id.rvSavingsBuckets)
        val txtBucketEmpty = view.findViewById<TextView>(R.id.txtSavingsBucketEmpty)
        val rvHistory = view.findViewById<RecyclerView>(R.id.rvSavingsHistory)
        val btnAdd = view.findViewById<View>(R.id.btnAddSaving)
        val btnBack = view.findViewById<View>(R.id.btnBack)

        val bucketAdapter = SavingsBucketAdapter()
        rvBuckets.layoutManager = LinearLayoutManager(requireContext())
        rvBuckets.adapter = bucketAdapter

        val historyAdapter = TransactionAdapter { transaction ->
            val intent = android.content.Intent(
                requireContext(),
                com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java
            )
            intent.putExtra("EXTRA_ID", transaction.id)
            startActivity(intent)
        }
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalSavings.collectLatest { balance ->
                txtTotal.text = com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(balance)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.savingBuckets.collectLatest { buckets ->
                bucketAdapter.submitList(buckets)
                txtBucketEmpty.visibility = if (buckets.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.savingTransactions.collectLatest { list ->
                historyAdapter.submitList(list)
            }
        }

        btnAdd.setOnClickListener {
            val intent = android.content.Intent(
                requireContext(),
                com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java
            )
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}

