package com.nhattien.expensemanager.ui.add

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.domain.Category
import com.nhattien.expensemanager.domain.TransactionType
import com.nhattien.expensemanager.domain.TypeGroup
import com.nhattien.expensemanager.ui.adapter.CategoryAdapter
import com.nhattien.expensemanager.viewmodel.AddTransactionViewModel
import com.nhattien.expensemanager.viewmodel.AddTransactionViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var viewModel: AddTransactionViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedCategory: Category? = null

    // Mặc định
    private var currentType = TransactionType.EXPENSE
    private var selectedDateInMillis: Long = System.currentTimeMillis()
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        viewModel = ViewModelProvider(this, AddTransactionViewModelFactory(application))[AddTransactionViewModel::class.java]

        // 1. Ánh xạ View
        val tabExpense = findViewById<TextView>(R.id.tabExpense)
        val tabIncome = findViewById<TextView>(R.id.tabIncome)
        val tabDebt = findViewById<TextView>(R.id.tabDebt)
        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
        val btnSave = findViewById<TextView>(R.id.btnSave)
        val btnClose = findViewById<View>(R.id.btnClose)
        val edtAmount = findViewById<EditText>(R.id.edtAmount)
        val edtNote = findViewById<EditText>(R.id.edtNote)

        // MỚI: Date & Recurring
        val btnSelectDate = findViewById<View>(R.id.btnSelectDate)
        val txtSelectedDate = findViewById<TextView>(R.id.txtSelectedDate)
        val swRecurring = findViewById<SwitchMaterial>(R.id.swRecurring)

        // 2. Setup Grid Danh mục
        categoryAdapter = CategoryAdapter { category ->
            selectedCategory = category

            // TỐI ƯU AI: Tự động bật "Cố định tháng" nếu chọn nhóm Chi Cố Định
            if (category.group == TypeGroup.EXPENSE_FIXED) {
                swRecurring.isChecked = true
                Toast.makeText(this, "Đã tự bật Cố định hàng tháng", Toast.LENGTH_SHORT).show()
            } else {
                swRecurring.isChecked = false
            }
        }
        rvCategories.layoutManager = GridLayoutManager(this, 4)
        rvCategories.adapter = categoryAdapter

        // 3. Hàm xử lý Tab
        fun switchTab(type: Int) {
            tabExpense.alpha = 0.5f
            tabIncome.alpha = 0.5f
            tabDebt.alpha = 0.5f

            // Xử lý danh sách hiển thị dựa trên Group mới
            val allCategories = Category.values()

            when (type) {
                0 -> { // CHI TIÊU
                    tabExpense.alpha = 1f
                    currentType = TransactionType.EXPENSE
                    // Lọc lấy: Cố định + Hằng ngày + Tiết kiệm (Gửi vào)
                    val list = allCategories.filter {
                        it.group == TypeGroup.EXPENSE_FIXED ||
                                it.group == TypeGroup.EXPENSE_DAILY ||
                                it.group == TypeGroup.SAVING // Gửi tiết kiệm cũng là chi tiền ra khỏi ví
                    }
                    categoryAdapter.submitList(list)
                }
                1 -> { // THU NHẬP
                    tabIncome.alpha = 1f
                    currentType = TransactionType.INCOME
                    // Lọc lấy: Income + Tiết kiệm (Rút ra)
                    val list = allCategories.filter {
                        it.group == TypeGroup.INCOME ||
                                it == Category.SAVING_OUT
                    }
                    categoryAdapter.submitList(list)
                }
                2 -> { // VAY / NỢ
                    tabDebt.alpha = 1f
                    // Type sẽ được xác định lại khi bấm Lưu
                    val list = allCategories.filter { it.group == TypeGroup.DEBT }
                    categoryAdapter.submitList(list)
                }
            }
        }

        // Mặc định tab 0
        switchTab(0)

        // Click Tab
        tabExpense.setOnClickListener { switchTab(0) }
        tabIncome.setOnClickListener { switchTab(1) }
        tabDebt.setOnClickListener { switchTab(2) }

        // 4. Xử lý Chọn ngày
        btnSelectDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDateInMillis = calendar.timeInMillis

                    // Cập nhật Text
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    txtSelectedDate.text = sdf.format(calendar.time)

                    // Logic nhỏ: Nếu chọn ngày hôm nay -> Hiện chữ "Hôm nay"
                    val today = Calendar.getInstance()
                    if (year == today.get(Calendar.YEAR) &&
                        month == today.get(Calendar.MONTH) &&
                        dayOfMonth == today.get(Calendar.DAY_OF_MONTH)) {
                        txtSelectedDate.text = "Hôm nay"
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // 5. Lưu
        btnSave.setOnClickListener {
            val amount = edtAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Tiền đâu?", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedCategory == null) {
                Toast.makeText(this, "Mua cái gì?", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Xử lý Type cuối cùng
            var finalType = currentType

            // Logic đặc biệt cho nhóm Nợ & Tiết kiệm
            when (selectedCategory) {
                Category.LENDING, Category.DEBT_REPAYMENT, Category.PAY_INTEREST, Category.SAVING_IN -> {
                    finalType = TransactionType.EXPENSE // Tiền đi
                }
                Category.BORROWING, Category.DEBT_COLLECTION, Category.INTEREST, Category.SAVING_OUT -> {
                    finalType = TransactionType.INCOME // Tiền về
                }
                else -> { /* Giữ nguyên */ }
            }

            // Gọi ViewModel (Chú ý: Cần update hàm addTransaction trong ViewModel để nhận date và isRecurring)
            // Tạm thời mình set vào Entity bên ViewModel, nhưng tốt nhất là sửa ViewModel sau
            // Ở đây mình giả định bạn sẽ sửa ViewModel để nhận thêm tham số, hoặc set date thẳng vào entity

            // Tạm thời sửa nhanh entity trong ViewModel hoặc tạo object ở đây
            viewModel.addTransaction(
                amount = amount,
                type = finalType,
                category = selectedCategory!!,
                note = edtNote.text.toString(),
                date = selectedDateInMillis, // Truyền ngày đã chọn
                isRecurring = swRecurring.isChecked // Truyền trạng thái lặp lại
            )

            Toast.makeText(this, "Đã lưu!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnClose.setOnClickListener { finish() }
    }
}