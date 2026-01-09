package com.nhattien.expensemanager.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.adapter.CalendarAdapter
import com.nhattien.expensemanager.ui.daydetail.DayDetailFragment
import com.nhattien.expensemanager.utils.DateUtils
import com.nhattien.expensemanager.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: CalendarAdapter
    private val calendar = Calendar.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Đảm bảo bạn đã có file res/layout/fragment_calendar.xml (từ bài trước)
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val rvCalendar = view.findViewById<RecyclerView>(R.id.rvCalendar)
        val txtMonthTitle = view.findViewById<TextView>(R.id.txtMonthTitle)
        val btnPrev = view.findViewById<View>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<View>(R.id.btnNextMonth)

        // SETUP ADAPTER
        adapter = CalendarAdapter { dayUi ->
            // ===> NÂNG CẤP: Bấm vào ngày -> Mở Chi tiết ngày <===
            if (dayUi.day > 0) {
                val fragment = DayDetailFragment.newInstance(dayUi.date)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null) // Để bấm Back quay lại được Lịch
                    .commit()
            }
        }

        rvCalendar.layoutManager = GridLayoutManager(context, 7)
        rvCalendar.adapter = adapter

        // HÀM UPDATE LỊCH
        fun updateCalendar(dailyTotals: Map<Int, Double>) {
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            txtMonthTitle.text = "Tháng $month / $year"

            // Tạo list ngày và đổ tiền vào
            val days = DateUtils.getDaysInMonth(month, year, dailyTotals)
            adapter.submitList(days)
        }

        // LẮNG NGHE DỮ LIỆU TỪ VIEWMODEL
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.calendarDailyTotals.collectLatest { totals ->
                updateCalendar(totals)
            }
        }

        // XỬ LÝ CHUYỂN THÁNG
        btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            // Trigger lại collect flow tự động cập nhật hoặc gọi hàm update với data hiện tại
            // Cách tốt nhất là ViewModel nên có biến currentMonth, nhưng tạm thời ta refresh lại UI với data cũ
            updateCalendar(viewModel.calendarDailyTotals.value)
        }
        btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar(viewModel.calendarDailyTotals.value)
        }
    }
}