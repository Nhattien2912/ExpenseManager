package com.nhattien.expensemanager.ui.debt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.adapter.DebtAdapter
import com.nhattien.expensemanager.viewmodel.DebtTab
import com.nhattien.expensemanager.viewmodel.DebtViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DebtFragment : Fragment() {

    private val viewModel: DebtViewModel by viewModels() // Use viewModels() as it's specific to this fragment, or activityViewModels if shared
    // Since DebtViewModel is new and specific, viewModels() is fine.
    
    // UI Elements
    private lateinit var txtTotalLabel: TextView
    private lateinit var txtTotalAmount: TextView
    private lateinit var rvDebt: RecyclerView
    private lateinit var btnTabReceivable: View
    private lateinit var btnTabPayable: View
    private lateinit var txtTabReceivable: TextView
    private lateinit var txtTabPayable: TextView
    private lateinit var indicatorReceivable: View
    private lateinit var indicatorPayable: View
    private lateinit var btnAdd: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_debt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun initViews(view: View) {
        txtTotalLabel = view.findViewById(R.id.txtSummaryLabel) ?: view.findViewById(R.id.txtTabReceivable) // Fallback if ID wrong, but logic below sets text
        // Note: txtTotalLabel is in the CardView which I added in fragment_debt.xml but didn't give ID.
        // Wait, I gave IDs for tabs. The CardView Text needs IDs.
        // I noticed in previous step I just pasted layout without IDs for Summary Texts?
        // Let's check fragment_debt.xml content I wrote in Step 773.
        // I reused IDs for tabs but didn't see IDs for "TỔNG CẦN THU" and "15.000.000".
        // I need to update fragment_debt.xml to add IDs for the summary card if I want to bind them!
        // Correction: I should update fragment_debt.xml first to ensure IDs exist.
        
        // Actually, let's look at the XML I wrote:
        // TextView text="TỔNG CẦN THU:" ... no ID. 
        // TextView text="15.000.000 đ" ... no ID.
        // I MUST FIX THE XML FIRST.
        
        rvDebt = view.findViewById(R.id.rvDebtList)
        btnTabReceivable = view.findViewById(R.id.btnTabReceivable)
        btnTabPayable = view.findViewById(R.id.btnTabPayable)
        txtTabReceivable = view.findViewById(R.id.txtTabReceivable)
        txtTabPayable = view.findViewById(R.id.txtTabPayable)
        indicatorReceivable = view.findViewById(R.id.indicatorReceivable)
        indicatorPayable = view.findViewById(R.id.indicatorPayable)
        // btnAdd = view.findViewById... (It's in the header, might not have ID)
    }

    private fun setupRecyclerView() {
        val adapter = DebtAdapter(
            onItemClick = { transaction ->
                val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
                intent.putExtra("EXTRA_ID", transaction.id)
                startActivity(intent)
            },
            onSettleClick = { transaction ->
                 viewModel.settleDebt(transaction)
                 android.widget.Toast.makeText(requireContext(), "Đã cập nhật trạng thái!", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
        rvDebt.layoutManager = LinearLayoutManager(requireContext())
        rvDebt.adapter = adapter
    }

    private fun setupListeners() {
        btnTabReceivable.setOnClickListener { viewModel.setTab(DebtTab.RECEIVABLE) }
        btnTabPayable.setOnClickListener { viewModel.setTab(DebtTab.PAYABLE) }
        
        // Add Button
        view?.findViewById<View>(R.id.btnAdd)?.setOnClickListener {
             // Navigate to AddTransaction with Loan type pre-selected?
             val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
             startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentTab.collectLatest { tab ->
                updateTabUI(tab)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentList.collectLatest { list ->
                (rvDebt.adapter as? DebtAdapter)?.submitList(list.map { it.transaction })
                // Update Empty View if needed
            }
        }
        
        // Update Total Amount based on Tab
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                viewModel.currentTab,
                viewModel.totalReceivable,
                viewModel.totalPayable
            ) { tab, receivable, payable -> Triple(tab, receivable, payable) }
            .collectLatest { (tab, receivable, payable) ->
                // Bind to UI
                 val (label, amount) = if (tab == DebtTab.RECEIVABLE) {
                    "TỔNG CẦN THU" to receivable
                } else {
                    "TỔNG CẦN TRẢ" to payable
                }
                
                // Need references to TextViews. I will bind them by finding by text or using correct IDs after I fix XML.
                // Assuming IDs: txtSummaryLabel, txtSummaryAmount
                view?.findViewById<TextView>(R.id.txtSummaryLabel)?.text = label
                 view?.findViewById<TextView>(R.id.txtSummaryAmount)?.text = amount.toCurrency()
            }
        }
    }

    private fun updateTabUI(tab: DebtTab) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.primary) // #2196F3
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_secondary) // #757575
        
        if (tab == DebtTab.RECEIVABLE) {
            txtTabReceivable.setTextColor(activeColor)
            indicatorReceivable.setBackgroundColor(activeColor)
            
            txtTabPayable.setTextColor(inactiveColor)
            indicatorPayable.setBackgroundResource(android.R.color.transparent)
        } else {
            txtTabReceivable.setTextColor(inactiveColor)
            indicatorReceivable.setBackgroundResource(android.R.color.transparent)
            
            txtTabPayable.setTextColor(activeColor)
            indicatorPayable.setBackgroundColor(activeColor)
        }
    }
}

fun Double.toCurrency(): String {
    return com.nhattien.expensemanager.utils.CurrencyUtils.toCurrency(this)
}
