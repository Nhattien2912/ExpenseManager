package com.nhattien.expensemanager.ui.budget

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.domain.Category
import com.nhattien.expensemanager.ui.adapter.TransactionAdapter
import com.nhattien.expensemanager.ui.add.AddTransactionActivity
import com.nhattien.expensemanager.utils.MoneyUtils
import com.nhattien.expensemanager.viewmodel.BudgetViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class BudgetFragment : Fragment() {

    private lateinit var viewModel: BudgetViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[BudgetViewModel::class.java]

        val txtReceivable = view.findViewById<TextView>(R.id.txtReceivable)
        val txtPayable = view.findViewById<TextView>(R.id.txtPayable)
        val rvDebt = view.findViewById<RecyclerView>(R.id.rvDebt)

        // Setup RecyclerView
        val adapter = TransactionAdapter { transaction ->
            // Khi bấm vào dòng nợ -> Hỏi đã xong chưa
            showSettleDialog(transaction)
        }
        rvDebt.layoutManager = LinearLayoutManager(requireContext())
        rvDebt.adapter = adapter
        rvDebt.layoutManager = LinearLayoutManager(requireContext())
        rvDebt.adapter = adapter

        // Quan sát dữ liệu từ ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalReceivable.collect { amount ->
                txtReceivable.text = MoneyUtils.format(amount)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalPayable.collect { amount ->
                txtPayable.text = MoneyUtils.format(amount)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.debtTransactions.collect { list ->
                adapter.submitList(list)
            }
        }
    }

    // Hiển thị hộp thoại hỏi "Đã trả chưa?"
    private fun showSettleDialog(transaction: com.nhattien.expensemanager.data.entity.TransactionEntity) {
        val message = when (transaction.category) {
            Category.LENDING -> "Người vay đã trả tiền cho bạn chưa?"
            Category.BORROWING -> "Bạn đã trả khoản nợ này chưa?"
            else -> {
                // Nếu là dòng khác (ví dụ dòng "Thu nợ") thì cho phép sửa như bình thường
                val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                intent.putExtra("TRANSACTION_ID", transaction.id)
                startActivity(intent)
                return
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận")
            .setMessage(message)
            .setPositiveButton("RỒI, ĐÃ XONG") { _, _ ->
                viewModel.settleDebt(transaction)
                Toast.makeText(context, "Đã cập nhật số dư!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Chưa", null)
            .show()
    }
}