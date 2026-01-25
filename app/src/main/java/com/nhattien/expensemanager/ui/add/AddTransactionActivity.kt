package com.nhattien.expensemanager.ui.add

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.domain.TransactionType
import com.nhattien.expensemanager.ui.adapter.CategoryAdapter
import com.nhattien.expensemanager.viewmodel.AddTransactionViewModel
import com.nhattien.expensemanager.viewmodel.AddTransactionViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var viewModel: AddTransactionViewModel
    private lateinit var categoryAdapter: CategoryAdapter
    private var selectedCategory: CategoryEntity? = null
    private var allCategories: List<CategoryEntity> = emptyList()

    private var currentType = TransactionType.EXPENSE
    private var selectedDateInMillis: Long = System.currentTimeMillis()
    private val calendar = Calendar.getInstance()

    // UI Elements
    private lateinit var tabExpense: TextView
    private lateinit var tabIncome: TextView
    private lateinit var tabDebt: TextView
    private lateinit var swipeRecurring: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // GLOBAL CRASH HANDLER
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runOnUiThread {
                android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.title_crash_report))
                    .setMessage(getString(R.string.msg_crash_error, throwable.message, android.util.Log.getStackTraceString(throwable)))
                    .setPositiveButton(getString(R.string.action_copy)) { _, _ ->
                        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Crash Log", android.util.Log.getStackTraceString(throwable))
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, getString(R.string.msg_copied), Toast.LENGTH_SHORT).show()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        try {
            setContentView(R.layout.activity_add_transaction)

            viewModel = ViewModelProvider(this, AddTransactionViewModelFactory(application))[AddTransactionViewModel::class.java]

            // 1. Map Views
            tabExpense = findViewById(R.id.tabExpense)
            tabIncome = findViewById(R.id.tabIncome)
            tabDebt = findViewById(R.id.tabDebt)
            val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
            val btnSave = findViewById<View>(R.id.btnSave)
            val btnClose = findViewById<View>(R.id.btnClose)
            val edtAmount = findViewById<EditText>(R.id.edtAmount)
            val edtNote = findViewById<EditText>(R.id.edtNote)
            val btnSelectDate = findViewById<View>(R.id.btnSelectDate)
            val txtSelectedDate = findViewById<TextView>(R.id.txtSelectedDate)
            swipeRecurring = findViewById(R.id.swRecurring)
            
            // Thêm TextWatcher để auto-format số tiền
            edtAmount.addTextChangedListener(com.nhattien.expensemanager.utils.CurrencyUtils.MoneyTextWatcher(edtAmount))

            // 2. Setup RecyclerView
            categoryAdapter = CategoryAdapter(
                onCategoryClick = { category ->
                    selectedCategory = category
                    categoryAdapter.setSelected(category)
                },
                onAddCategoryClick = {
                    showAddCategoryDialog()
                }
            )
            rvCategories.layoutManager = GridLayoutManager(this, 4)
            rvCategories.adapter = categoryAdapter

            // 3. Observe Data
            viewModel.allCategories.observe(this) { list ->
                allCategories = list
                updateCategoryList()
            }

            // 4. Tab Logic
            // Helper to toggle visibility
            fun updateTabUI(type: TransactionType) {
                // ... (Existing tab UI logic for colors is inside switchTab or similar, assuming switchTab handles it)
                // Just handle Contact visibility here based on type
                val contactLayout = findViewById<android.view.View>(R.id.layoutContact)
                val contactDivider = findViewById<android.view.View>(R.id.dividerContact)
                
                if (type == TransactionType.LOAN_GIVE || type == TransactionType.LOAN_TAKE) {
                    contactLayout.visibility = android.view.View.VISIBLE
                    contactDivider.visibility = android.view.View.VISIBLE
                    
                    // Update label based on context if possible, but sticking to "Người liên quan" or generic might be easier
                    // But selectedCategory tells us more.
                } else {
                    contactLayout.visibility = android.view.View.GONE
                    contactDivider.visibility = android.view.View.GONE
                }
            }

            tabExpense.setOnClickListener { switchTab(TransactionType.EXPENSE); updateTabUI(TransactionType.EXPENSE) }
            tabIncome.setOnClickListener { switchTab(TransactionType.INCOME); updateTabUI(TransactionType.INCOME) }
            tabDebt.setOnClickListener { switchTab(TransactionType.LOAN_GIVE); updateTabUI(TransactionType.LOAN_GIVE) }

            // 5. Date Selection
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

            // 6. Save
            btnSave.setOnClickListener {
                try {
                    // Parse số từ format có dấu phân cách
                    val amount = com.nhattien.expensemanager.utils.CurrencyUtils.parseFromSeparator(edtAmount.text.toString())
                    if (amount <= 0) {
                        Toast.makeText(this, R.string.error_enter_amount, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (selectedCategory == null) {
                        Toast.makeText(this, R.string.error_select_category, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val finalType = selectedCategory!!.type
                    
                    // Handle Contact Name
                    var finalNote = edtNote.text.toString()
                    val contactName = findViewById<android.widget.EditText>(R.id.edtContact).text.toString()
                    val contactLayout = findViewById<android.view.View>(R.id.layoutContact)
                    
                    if (contactLayout.visibility == android.view.View.VISIBLE && contactName.isNotBlank()) {
                         finalNote = "Liên quan: $contactName. $finalNote"
                    }

                    viewModel.addTransaction(
                        amount = amount,
                        type = finalType,
                        categoryId = selectedCategory!!.id,
                        paymentMethod = "CASH", // Default for now
                        note = finalNote,
                        date = selectedDateInMillis,
                        isRecurring = swipeRecurring.isChecked,
                        onSuccess = {
                            Toast.makeText(this, R.string.msg_transaction_added, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    )
                } catch (e: Exception) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.title_error_save))
                        .setMessage(e.toString())
                        .setPositiveButton("OK", null)
                        .show()
                }
            }

            btnClose.setOnClickListener { finish() }

            // Initial state
            switchTab(TransactionType.EXPENSE)
        
        } catch (e: Exception) {
            e.printStackTrace()
            android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_error_init))
                .setMessage("Chi tiết lỗi: " + e.message + "\n\n" + e.stackTraceToString())
                .setPositiveButton(getString(R.string.action_close)) { _, _ -> finish() }
                .show()
        }
    }

    private fun switchTab(type: TransactionType) {
        currentType = type
        
        val colorSecondary = ContextCompat.getColor(this, R.color.text_secondary)
        val colorWhite = ContextCompat.getColor(this, R.color.text_white)
        val colorPrimaryText = ContextCompat.getColor(this, R.color.text_primary)

        // Reset all tabs to default state (inactive)
        tabExpense.setBackgroundResource(R.drawable.bg_tab_inactive_left)
        tabIncome.setBackgroundResource(R.drawable.bg_tab_inactive_center)
        tabDebt.setBackgroundResource(R.drawable.bg_tab_inactive_right)
        
        tabExpense.setTextColor(colorSecondary)
        tabIncome.setTextColor(colorSecondary)
        tabDebt.setTextColor(colorSecondary)

        // Set active state for selected tab
        when (type) {
             TransactionType.EXPENSE -> {
                tabExpense.setBackgroundResource(R.drawable.bg_tab_active_expense)
                tabExpense.setTextColor(colorWhite)
             }
             TransactionType.INCOME -> {
                tabIncome.setBackgroundResource(R.drawable.bg_tab_active_income)
                tabIncome.setTextColor(colorWhite)
             }
             TransactionType.LOAN_GIVE, TransactionType.LOAN_TAKE -> {
                tabDebt.setBackgroundResource(R.drawable.bg_tab_active_debt)
                tabDebt.setTextColor(colorWhite)
             }
        }
        
        selectedCategory = null
        categoryAdapter.setSelected(null)
        updateCategoryList()
    }

    private fun updateCategoryList() {
        val filtered = if (currentType == TransactionType.LOAN_GIVE || currentType == TransactionType.LOAN_TAKE) {
            allCategories.filter { it.type == TransactionType.LOAN_GIVE || it.type == TransactionType.LOAN_TAKE }
        } else {
            allCategories.filter { it.type == currentType }
        }
        categoryAdapter.submitList(filtered)
    }
    
    /**
     * Hiển thị dialog thêm danh mục mới
     */
    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category, null)
        val edtIcon = dialogView.findViewById<EditText>(R.id.edtCategoryIcon)
        val edtName = dialogView.findViewById<EditText>(R.id.edtCategoryName)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)
        val btnAdd = dialogView.findViewById<View>(R.id.btnAdd)
        
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        btnAdd.setOnClickListener {
            val icon = edtIcon.text.toString().trim()
            val name = edtName.text.toString().trim()
            
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.hint_category_name_input), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (icon.isEmpty()) {
                Toast.makeText(this, getString(R.string.hint_category_icon_input), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Thêm danh mục mới với type hiện tại
            try {
                viewModel.addCategory(
                    name = name,
                    icon = icon,
                    type = currentType,
                    onSuccess = {
                        Toast.makeText(this, "${getString(R.string.msg_category_added)}: $name", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
}
