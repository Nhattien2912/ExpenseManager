package com.nhattien.expensemanager.ui.debt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.adapter.DebtAdapter
import com.nhattien.expensemanager.viewmodel.BudgetViewModel // Tạm dùng lại BudgetViewModel hoặc tạo DebtViewModel mới

class DebtFragment : Fragment() {

    private lateinit var viewModel: BudgetViewModel // Sau này đổi thành DebtViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Tạm thời dùng layout trống hoặc layout budget cũ,
        // Lần tới ta sẽ vẽ layout fragment_debt.xml đẹp hơn
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo Adapter
        val adapter = DebtAdapter { debt ->
            Toast.makeText(context, "Đã xong khoản nợ của ${debt.debtorName}", Toast.LENGTH_SHORT).show()
            // Gọi ViewModel để update trạng thái isFinished = true
        }

        val rvDebt = view.findViewById<RecyclerView>(R.id.rvDebt)
        rvDebt.layoutManager = LinearLayoutManager(context)
        rvDebt.adapter = adapter
        view.findViewById<View>(R.id.fabAddDebt).setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.nhattien.expensemanager.ui.add.AddDebtActivity::class.java)
            startActivity(intent)
        }
        // TODO: Kết nối ViewModel để lấy list DebtEntity thật
    }
}