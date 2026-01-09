package com.nhattien.expensemanager.ui.add

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.viewmodel.AddDebtViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddDebtActivity : AppCompatActivity() {

    private lateinit var viewModel: AddDebtViewModel
    private var isMeLending = true // Mặc định: Mình cho vay
    private var selectedDueDate: Long? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_debt)

        viewModel = ViewModelProvider(this)[AddDebtViewModel::class.java]

        val tabLend = findViewById<TextView>(R.id.tabLend)
        val tabBorrow = findViewById<TextView>(R.id.tabBorrow)
        val btnSave = findViewById<TextView>(R.id.btnSave)
        val btnClose = findViewById<android.view.View>(R.id.btnClose)
        val btnDueDate = findViewById<android.view.View>(R.id.btnDueDate)
        val txtDueDate = findViewById<TextView>(R.id.txtDueDate)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtAmount = findViewById<EditText>(R.id.edtAmount)
        val edtInterest = findViewById<EditText>(R.id.edtInterest)

        // 1. Xử lý Tab
        fun switchTab(lend: Boolean) {
            isMeLending = lend
            if (lend) {
                tabLend.alpha = 1f
                tabBorrow.alpha = 0.5f
                // Có thể đổi màu nền động nếu muốn
            } else {
                tabLend.alpha = 0.5f
                tabBorrow.alpha = 1f
            }
        }
        switchTab(true) // Mặc định

        tabLend.setOnClickListener { switchTab(true) }
        tabBorrow.setOnClickListener { switchTab(false) }

        // 2. Chọn ngày
        btnDueDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDueDate = calendar.timeInMillis

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                txtDueDate.text = sdf.format(calendar.time)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 3. Lưu
        btnSave.setOnClickListener {
            val name = edtName.text.toString()
            val amount = edtAmount.text.toString().toDoubleOrNull()
            val interest = edtInterest.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isBlank() || amount == null || amount <= 0) {
                Toast.makeText(this, "Vui lòng nhập tên và số tiền!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addDebt(name, amount, isMeLending, selectedDueDate, interest)
            Toast.makeText(this, "Đã lưu sổ nợ!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnClose.setOnClickListener { finish() }
    }
}