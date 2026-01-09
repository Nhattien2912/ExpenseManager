package com.nhattien.expensemanager.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.adapter.TransactionAdapter
import com.nhattien.expensemanager.utils.MoneyUtils
import com.nhattien.expensemanager.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainFragment : Fragment(R.layout.fragment_main) { // Dùng constructor layout cho gọn

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: TransactionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        setupShortcutButtons(view)
        setupRecyclerView(view)
        observeData(view)
    }

    private fun setupShortcutButtons(view: View) {
        // 1. Sổ Nợ
        val btnDebt = view.findViewById<View>(R.id.btnShortcutDebt)
        btnDebt.findViewById<TextView>(R.id.txtTitle).text = "Sổ Nợ"
        btnDebt.findViewById<ImageView>(R.id.imgIcon).apply {
            setImageResource(R.drawable.ic_wallet) // Thay icon sổ nếu có
            setColorFilter(requireContext().getColor(R.color.debt)) // Màu cam
            background.setTint(0xFFFFF3E0.toInt()) // Nền cam nhạt
        }
        btnDebt.setOnClickListener { Toast.makeText(context, "Mở Sổ Nợ", Toast.LENGTH_SHORT).show() }

        // 2. Tiết kiệm
        val btnSaving = view.findViewById<View>(R.id.btnShortcutSavings)
        btnSaving.findViewById<TextView>(R.id.txtTitle).text = "Tiết kiệm"
        btnSaving.findViewById<ImageView>(R.id.imgIcon).apply {
            setImageResource(R.drawable.ic_wallet) // Thay icon heo đất
            setColorFilter(requireContext().getColor(R.color.saving)) // Màu tím
            background.setTint(0xFFF3E5F5.toInt()) // Nền tím nhạt
        }
        btnSaving.setOnClickListener { Toast.makeText(context, "Mở Tiết kiệm", Toast.LENGTH_SHORT).show() }

        // 3. Ngân sách
        val btnBudget = view.findViewById<View>(R.id.btnShortcutBudget)
        btnBudget.findViewById<TextView>(R.id.txtTitle).text = "Ngân sách"
        btnBudget.findViewById<ImageView>(R.id.imgIcon).apply {
            setImageResource(R.drawable.ic_chart)
            setColorFilter(requireContext().getColor(R.color.primary)) // Màu xanh
            background.setTint(0xFFE3F2FD.toInt()) // Nền xanh nhạt
        }

        // 4. Đổi tiền
        val btnExchange = view.findViewById<View>(R.id.btnShortcutExchange)
        btnExchange.findViewById<TextView>(R.id.txtTitle).text = "Đổi tiền"
        btnExchange.findViewById<ImageView>(R.id.imgIcon).apply {
            setImageResource(R.drawable.ic_settings)
            setColorFilter(0xFF607D8B.toInt()) // Màu xám
            background.setTint(0xFFECEFF1.toInt())
        }
    }

    private fun setupRecyclerView(view: View) {
        val rvRecent = view.findViewById<RecyclerView>(R.id.rvRecentTransactions)

        // KHI CLICK VÀO 1 GIAO DỊCH
        adapter = TransactionAdapter { transaction ->
            // Hiện Dialog Sửa/Xóa
            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.layout_dialog_transaction, null)

            val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            // Điền dữ liệu vào Dialog
            dialogView.findViewById<TextView>(R.id.txtDialogCategory).text = transaction.category.label
            dialogView.findViewById<TextView>(R.id.txtDialogAmount).text = MoneyUtils.format(transaction.amount)
            dialogView.findViewById<TextView>(R.id.txtDialogNote).text = transaction.note

            // NÚT XÓA
            dialogView.findViewById<View>(R.id.btnDelete).setOnClickListener {
                viewModel.deleteTransaction(transaction) // Cần thêm hàm này vào MainViewModel
                dialog.dismiss()
                Toast.makeText(context, "Đã xóa!", Toast.LENGTH_SHORT).show()
            }

            // NÚT SỬA
            dialogView.findViewById<View>(R.id.btnEdit).setOnClickListener {
                val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
                intent.putExtra("TRANSACTION_ID", transaction.id) // Gửi ID sang để sửa
                startActivity(intent)
                dialog.dismiss()
            }

            dialog.show()
        }

        rvRecent.layoutManager = LinearLayoutManager(context)
        rvRecent.adapter = adapter
    }

    private fun observeData(view: View) {
        val txtTotalBalance = view.findViewById<TextView>(R.id.txtTotalBalance)
        val txtToday = view.findViewById<TextView>(R.id.txtTodayBalance)
        val txtMonth = view.findViewById<TextView>(R.id.txtMonthBalance)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recentTransactions.collectLatest { adapter.submitList(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalBalance.collectLatest {
                txtTotalBalance.text = MoneyUtils.format(it)
            }
        }

        // Cần thêm Logic tính "Hôm nay" và "Tháng này" vào ViewModel sau
        // Tạm thời hiển thị giả lập hoặc lấy dữ liệu thật nếu ViewModel đã có
    }
}