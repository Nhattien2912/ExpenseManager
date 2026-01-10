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
import com.nhattien.expensemanager.ui.daydetail.DayDetailFragment // Đảm bảo đã import đúng
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
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val rvCalendar = view.findViewById<RecyclerView>(R.id.rvCalendar)
        val txtMonthTitle = view.findViewById<TextView>(R.id.txtMonthTitle)
        val btnPrev = view.findViewById<View>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<View>(R.id.btnNextMonth)

        // SETUP ADAPTER & SỰ KIỆN CLICK NGÀY
        adapter = CalendarAdapter { dayUi ->
            if (dayUi.day > 0) {
                // ===> CHỖ NÀY LÀM LOGIC "NÂNG CẤP CÁI DƯỚI" <===
                // Khi bấm vào ngày -> Mở Fragment chi tiết (DayDetailFragment)
                val fragment = DayDetailFragment.newInstance(dayUi.date)

                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, fragment) // Thay thế fragment hiện tại
                    .addToBackStack(null) // Để user bấm Back sẽ quay lại Lịch
                    .commit()
            }
        }

        rvCalendar.layoutManager = GridLayoutManager(context, 7)
        rvCalendar.adapter = adapter

        // HÀM CẬP NHẬT LỊCH
        fun updateCalendar(dailyTotals: Map<Int, Double>) {
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            txtMonthTitle.text = "Tháng $month / $year"

            // Lấy danh sách ngày từ DateUtils và đổ tiền vào
            val days = DateUtils.getDaysInMonth(month, year, dailyTotals)
            adapter.submitList(days)
        }

        // LẮNG NGHE DỮ LIỆU TỪ VIEWMODEL
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.calendarDailyTotals.collectLatest { totals ->
                updateCalendar(totals)
            }
        }

        // CHUYỂN THÁNG
        btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar(viewModel.calendarDailyTotals.value)
        }
        btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar(viewModel.calendarDailyTotals.value)
        }
    }
}