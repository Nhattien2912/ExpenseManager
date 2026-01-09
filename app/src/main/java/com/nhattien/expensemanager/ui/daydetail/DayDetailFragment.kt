package com.nhattien.expensemanager.ui.daydetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.adapter.TransactionAdapter
//import com.nhattien.expensemanager.viewmodel.DayDetailViewModel
import com.nhattien.expensemanager.viewmodel.DayDetailViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DayDetailFragment : Fragment() {

    // ===== NHẬN DATE TỪ MAIN =====
    companion object {
        private const val ARG_DATE = "arg_date"

        fun newInstance(date: String): DayDetailFragment {
            val fragment = DayDetailFragment()
            val args = Bundle()
            args.putString(ARG_DATE, date)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_day_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ===== FIND VIEW =====
        val txtDate = view.findViewById<TextView>(R.id.txtDate)
        val txtIncome = view.findViewById<TextView>(R.id.txtIncomeDay)
        val txtExpense = view.findViewById<TextView>(R.id.txtExpenseDay)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        // ===== SETUP LIST =====
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // [SỬA LẠI ĐOẠN NÀY]
        val adapter = TransactionAdapter { transaction ->
            // Sự kiện khi bấm vào item (để trống hoặc xử lý sau)
        }

        // ---> THÊM DÒNG NÀY <---
        recyclerView.adapter = adapter

        // ===== LẤY DATE TRUYỀN VÀO =====
        val dateStr = arguments?.getString(ARG_DATE)
            ?: return

        txtDate.text = dateStr

        // ===== yyyy-MM-dd → startOfDay / endOfDay =====
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr)!!

        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis - 1

        // ===== VIEWMODEL =====
        val viewModel = ViewModelProvider(
            this,
            DayDetailViewModelFactory(
                requireActivity().application,
                startOfDay,
                endOfDay
            )
        )[DayDetailViewModel::class.java]

        // ===== COLLECT DATA =====
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.transactions.collect {
                adapter.submitList(it)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalIncome.collect {
                txtIncome.text = "Thu: $it"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalExpense.collect {
                txtExpense.text = "Chi: $it"
            }
        }
    }
}
