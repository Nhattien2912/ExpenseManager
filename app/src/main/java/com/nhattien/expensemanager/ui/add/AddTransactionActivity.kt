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

import androidx.core.content.ContextCompat

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var viewModel: AddTransactionViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedCategory: Category? = null

    private var currentType = TransactionType.EXPENSE
    private var selectedDateInMillis: Long = System.currentTimeMillis()
    private val calendar = Calendar.getInstance()
    private var transactionId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        viewModel = ViewModelProvider(this, AddTransactionViewModelFactory(application))[AddTransactionViewModel::class.java]

        // 1. Ánh xạ View
        val tabExpense = findViewById<TextView>(R.id.tabExpense)
        val tabIncome = findViewById<TextView>(R.id.tabIncome)
        val tabDebt = findViewById<TextView>(R.id.tabDebt)
        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
        val btnSave = findViewById<View>(R.id.btnSave)
        val btnClose = findViewById<View>(R.id.btnClose)
        val edtAmount = findViewById<EditText>(R.id.edtAmount)
        val edtNote = findViewById<EditText>(R.id.edtNote)
        val btnSelectDate = findViewById<View>(R.id.btnSelectDate)
        val txtSelectedDate = findViewById<TextView>(R.id.txtSelectedDate)
        val swRecurring = findViewById<SwitchMaterial>(R.id.swRecurring)

        // 2. Setup RecyclerView
        categoryAdapter = CategoryAdapter { category ->
            selectedCategory = category
            if (category.group == TypeGroup.EXPENSE_FIXED) {
                swRecurring.isChecked = true
            }
        }
        rvCategories.layoutManager = GridLayoutManager(this, 4)
        rvCategories.adapter = categoryAdapter



        // 3. Logic xử lý Tab
        fun switchTab(index: Int) {
            // Reset background (Default to Inactive)
            tabExpense.setBackgroundResource(R.drawable.bg_tab_expense_inactive)
            tabIncome.setBackgroundResource(R.drawable.bg_tab_income_inactive)
            tabDebt.setBackgroundResource(0) // Reset to transparent
            
            val colorSecondary = ContextCompat.getColor(this, R.color.text_secondary)
            val colorWhite = ContextCompat.getColor(this, R.color.text_white)
            val colorPrimaryText = ContextCompat.getColor(this, R.color.text_primary)

            tabExpense.setTextColor(colorSecondary)
            tabIncome.setTextColor(colorSecondary)
            tabDebt.setTextColor(colorSecondary)

            val allCategories = Category.values()
            when (index) {
                0 -> {
                    tabExpense.setBackgroundResource(R.drawable.bg_tab_expense_active)
                    tabExpense.setTextColor(colorWhite)
                    currentType = TransactionType.EXPENSE
                    categoryAdapter.submitList(allCategories.filter { 
                        it.group == TypeGroup.EXPENSE_FIXED || it.group == TypeGroup.EXPENSE_DAILY || it.group == TypeGroup.SAVING 
                    })
                }
                1 -> {
                    tabIncome.setBackgroundResource(R.drawable.bg_tab_income_active)
                    tabIncome.setTextColor(colorWhite)
                    currentType = TransactionType.INCOME
                    categoryAdapter.submitList(allCategories.filter { 
                        it.group == TypeGroup.INCOME || it == Category.SAVING_OUT 
                    })
                }
                2 -> {
                    tabDebt.setBackgroundResource(R.drawable.bg_today)
                    tabDebt.setTextColor(colorPrimaryText)
                    currentType = TransactionType.LOAN_GIVE // Default placeholder
                    categoryAdapter.submitList(allCategories.filter { it.group == TypeGroup.DEBT })
                }
            }
            selectedCategory = null
        }

        switchTab(0)

        tabExpense.setOnClickListener { switchTab(0) }
        tabIncome.setOnClickListener { switchTab(1) }
        tabDebt.setOnClickListener { switchTab(2) }

        // KHỞI TẠO LOGIC EDIT
        transactionId = intent.getLongExtra("EXTRA_ID", -1L)
        if (transactionId != -1L) {
            findViewById<TextView>(R.id.txtTitle).text = "Sửa giao dịch"
            viewModel.getTransaction(transactionId)
            viewModel.transaction.observe(this) { transaction ->
                if (transaction != null) {
                    val formatter = java.text.DecimalFormat("#,###")
                    val symbols = java.text.DecimalFormatSymbols(Locale("vi", "VN"))
                    symbols.groupingSeparator = '.'
                    formatter.decimalFormatSymbols = symbols
                    edtAmount.setText(formatter.format(transaction.amount))
                    edtNote.setText(transaction.note)
                    swRecurring.isChecked = transaction.isRecurring
                    
                    // Set Date
                    calendar.timeInMillis = transaction.date
                    selectedDateInMillis = transaction.date
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    txtSelectedDate.text = sdf.format(transaction.date)

                    // Set Type & Tab
                    when (transaction.type) {
                        TransactionType.EXPENSE -> switchTab(0)
                        TransactionType.INCOME -> switchTab(1)
                        TransactionType.LOAN_GIVE, TransactionType.LOAN_TAKE -> switchTab(2)
                    }
                    
                    // Select Category
                    selectedCategory = transaction.category
                    categoryAdapter.setSelected(transaction.category)
                }
            }
        }

        // 4. Chọn ngày
        btnSelectDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                calendar.set(y, m, d)
                selectedDateInMillis = calendar.timeInMillis
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                txtSelectedDate.text = if (Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)) 
                    getString(R.string.date_today) 
                else 
                    sdf.format(calendar.time)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 6. Format SỐ TIỀN trong lúc nhập (TextWatcher)
        edtAmount.addTextChangedListener(object : android.text.TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.toString() != current) {
                    edtAmount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[^\\d]".toRegex(), "")
                    
                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toDouble()
                        // Format: 10.000.000 (Vietnam Style)
                        val formatter = java.text.DecimalFormat("#,###")
                        val symbols = java.text.DecimalFormatSymbols(Locale("vi", "VN"))
                        symbols.groupingSeparator = '.'
                        formatter.decimalFormatSymbols = symbols
                        
                        val formatted = formatter.format(parsed)
                        current = formatted
                        edtAmount.setText(formatted)
                        edtAmount.setSelection(formatted.length)
                    } else {
                        current = ""
                        edtAmount.setText("")
                    }

                    edtAmount.addTextChangedListener(this)
                }
            }
        })

        // 5. Lưu
        btnSave.setOnClickListener {
            // Remove dots before parsing
            val cleanAmount = edtAmount.text.toString().replace(".", "").replace(",", "")
            val amount = cleanAmount.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, R.string.error_enter_amount, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedCategory == null) {
                Toast.makeText(this, R.string.error_select_category, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var finalType = currentType
            when (selectedCategory) {
                // Mapping Specific Debt Types
                Category.LENDING, Category.DEBT_REPAYMENT -> finalType = TransactionType.LOAN_GIVE
                Category.BORROWING, Category.DEBT_COLLECTION -> finalType = TransactionType.LOAN_TAKE
                
                // Existing Mapping
                Category.PAY_INTEREST, Category.SAVING_IN -> finalType = TransactionType.EXPENSE
                Category.INTEREST, Category.SAVING_OUT -> finalType = TransactionType.INCOME
                else -> {}
            }

            if (transactionId != -1L) {
                viewModel.updateTransaction(
                    id = transactionId,
                    amount = amount,
                    type = finalType,
                    category = selectedCategory!!,
                    note = edtNote.text.toString(),
                    date = selectedDateInMillis,
                    isRecurring = swRecurring.isChecked
                ) {
                    Toast.makeText(this, "Đã cập nhật giao dịch", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                viewModel.addTransaction(
                    amount = amount,
                    type = finalType,
                    category = selectedCategory!!,
                    note = edtNote.text.toString(),
                    date = selectedDateInMillis,
                    isRecurring = swRecurring.isChecked
                ) {
                    Toast.makeText(this, R.string.msg_transaction_added, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        btnClose.setOnClickListener { finish() }
    }
}
