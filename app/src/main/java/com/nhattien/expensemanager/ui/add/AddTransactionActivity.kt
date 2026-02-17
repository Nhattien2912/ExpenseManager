package com.nhattien.expensemanager.ui.add

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.entity.TagEntity
import com.nhattien.expensemanager.data.entity.WalletEntity
import com.nhattien.expensemanager.domain.TransactionType
import com.nhattien.expensemanager.ui.adapter.CategoryAdapter
import com.nhattien.expensemanager.ui.adapter.WalletSpinnerAdapter
import com.nhattien.expensemanager.ui.tag.ManageTagsActivity
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
    
    // Tag Logic
    private var allTagsList: List<TagEntity> = emptyList()
    private val selectedTags = mutableListOf<TagEntity>()
    private lateinit var chipGroupTags: ChipGroup

    private var currentType = TransactionType.EXPENSE
    private var selectedDateInMillis: Long = System.currentTimeMillis()
    private val calendar = Calendar.getInstance()

    // UI Elements
    private lateinit var tabExpense: TextView
    private lateinit var tabIncome: TextView
    private lateinit var tabDebt: TextView
    private lateinit var tabTransfer: TextView // Added
    
    private lateinit var spinnerWallet: android.widget.Spinner // Added
    private lateinit var spinnerTargetWallet: android.widget.Spinner // Added
    private lateinit var layoutTargetWallet: View // Added
    private lateinit var lblSourceWallet: TextView // Added
    private lateinit var swipeRecurring: SwitchMaterial
    private lateinit var edtAmount: EditText
    private lateinit var edtNote: EditText
    private lateinit var txtSelectedDate: TextView
    private lateinit var btnSelectDate: View
    private lateinit var btnSave: View
    private lateinit var btnClose: View
    private lateinit var btnDelete: View
    private lateinit var txtTitle: TextView
    
    // Wallets Data
    private var allWallets: List<WalletEntity> = emptyList()
    private lateinit var walletAdapter: WalletSpinnerAdapter
    private lateinit var targetWalletAdapter: WalletSpinnerAdapter
    
    private var selectedWalletId: Long = 1L
    private var selectedTargetWalletId: Long? = null
    private var pendingWalletIdForEdit: Long? = null
    private var pendingTargetWalletIdForEdit: Long? = null
    private var didInitDefaultWalletSelection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // GLOBAL CRASH HANDLER
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            runOnUiThread {
                android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.title_crash_report))
                    .setMessage(throwable.message + "\n" + android.util.Log.getStackTraceString(throwable))
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
            tabTransfer = findViewById(R.id.tabTransfer) // Added
            
            spinnerWallet = findViewById(R.id.spinnerWallet) // Added
            spinnerTargetWallet = findViewById(R.id.spinnerTargetWallet) // Added
            layoutTargetWallet = findViewById(R.id.layoutTargetWallet) // Added
            lblSourceWallet = findViewById(R.id.lblSourceWallet) // Added

            val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
            btnSave = findViewById(R.id.btnSave)
            btnClose = findViewById(R.id.btnClose)
            btnDelete = findViewById(R.id.btnDelete)
            edtAmount = findViewById(R.id.edtAmount)
            edtNote = findViewById(R.id.edtNote)
            btnSelectDate = findViewById(R.id.btnSelectDate)
            txtSelectedDate = findViewById(R.id.txtSelectedDate)
            txtTitle = findViewById(R.id.txtTitle)
            swipeRecurring = findViewById(R.id.swRecurring)
            
            // Tag Views
            chipGroupTags = findViewById(R.id.chipGroupTags)
            val btnAddTag = findViewById<View>(R.id.btnAddTag)
            
            // Check Edit Mode
            val editId = intent.getLongExtra("EXTRA_ID", -1L)
            val isEditMode = editId != -1L

            if (isEditMode) {
                txtTitle.text = "Chỉnh sửa giao dịch" 
                btnDelete.visibility = View.VISIBLE
                
                // Load Data
                viewModel.getTransaction(editId)
            } else {
                btnDelete.visibility = View.GONE
            }
            
            // TextWatcher for Amount
            edtAmount.addTextChangedListener(com.nhattien.expensemanager.utils.CurrencyUtils.MoneyTextWatcher(edtAmount))

            // 2. Setup RecyclerView (Category)
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

            // 3. Setup Spinners (Wallets)
            // Init with empty list, will update when observed
            walletAdapter = WalletSpinnerAdapter(this, emptyList())
            spinnerWallet.adapter = walletAdapter
            
            targetWalletAdapter = WalletSpinnerAdapter(this, emptyList())
            spinnerTargetWallet.adapter = targetWalletAdapter
            
            spinnerWallet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (allWallets.isNotEmpty()) {
                        selectedWalletId = allWallets[position].id
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            
            spinnerTargetWallet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (allWallets.isNotEmpty()) {
                        selectedTargetWalletId = allWallets[position].id
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // 4. Observe Data
            
            // Observe Wallets
            viewModel.allWallets.observe(this) { wallets ->
                allWallets = wallets
                walletAdapter = WalletSpinnerAdapter(this, wallets)
                spinnerWallet.adapter = walletAdapter

                targetWalletAdapter = WalletSpinnerAdapter(this, wallets)
                spinnerTargetWallet.adapter = targetWalletAdapter

                if (wallets.isEmpty()) return@observe

                if (isEditMode) {
                    applyPendingWalletSelection()
                    return@observe
                }

                if (!didInitDefaultWalletSelection) {
                    val defaultIndex = wallets.indexOfFirst { it.id == 1L }.takeIf { it >= 0 } ?: 0
                    spinnerWallet.setSelection(defaultIndex)
                    selectedWalletId = wallets[defaultIndex].id

                    if (wallets.size > 1) {
                        val targetIndex = if (defaultIndex == 0) 1 else 0
                        spinnerTargetWallet.setSelection(targetIndex)
                        selectedTargetWalletId = wallets[targetIndex].id
                    } else {
                        selectedTargetWalletId = null
                    }

                    didInitDefaultWalletSelection = true
                }
            }

            viewModel.allCategories.observe(this) { list ->
                allCategories = list
                updateCategoryList()
            }
            
            viewModel.allTags.observe(this) { tags ->
                allTagsList = tags
            }
            
            viewModel.transaction.observe(this) { transactionWithCategory ->
                if (transactionWithCategory != null && isEditMode) {
                    val trx = transactionWithCategory.transaction
                    val cat = transactionWithCategory.category
                    
                    edtAmount.setText(com.nhattien.expensemanager.utils.CurrencyUtils.formatWithSeparator(trx.amount))
                    
                    val note = trx.note
                    if (note.startsWith("Liên quan: ")) {
                         if (note.contains(". ")) {
                             val parts = note.split(". ", limit = 2)
                             val contactPart = parts[0].removePrefix("Liên quan: ")
                             val realNote = if (parts.size > 1) parts[1] else ""
                             edtNote.setText(realNote)
                             findViewById<EditText>(R.id.edtContact).setText(contactPart)
                         } else {
                             edtNote.setText(note)
                         }
                    } else {
                        edtNote.setText(note)
                    }

                    selectedDateInMillis = trx.date
                    calendar.timeInMillis = trx.date
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    txtSelectedDate.text = sdf.format(calendar.time)
                    
                    swipeRecurring.isChecked = trx.isRecurring
                    
                    pendingWalletIdForEdit = trx.walletId
                    pendingTargetWalletIdForEdit = trx.targetWalletId
                    applyPendingWalletSelection()
                    
                    switchTab(trx.type)
                    
                    selectedCategory = cat
                    categoryAdapter.setSelected(cat)
                }
            }
            
            viewModel.transactionTags.observe(this) { tags ->
                 if (isEditMode) {
                     selectedTags.clear()
                     selectedTags.addAll(tags)
                     refreshTagChips()
                 }
            }

            // 5. Setup Listeners
            tabExpense.setOnClickListener { switchTab(TransactionType.EXPENSE) }
            tabIncome.setOnClickListener { switchTab(TransactionType.INCOME) }
            tabDebt.setOnClickListener { switchTab(TransactionType.LOAN_GIVE) }
            tabTransfer.setOnClickListener { switchTab(TransactionType.TRANSFER) } // Added

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
            
            btnAddTag.setOnClickListener {
                showTagSelectionDialog()
            }

            btnSave.setOnClickListener { saveTransaction(isEditMode, editId) }
            
             btnDelete.setOnClickListener {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa giao dịch này không?")
                    .setPositiveButton("Xóa") { _, _ ->
                        viewModel.deleteTransaction(editId) {
                            Toast.makeText(this, "Đã xóa giao dịch", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }

            btnClose.setOnClickListener { finish() }

            if (!isEditMode) {
                switchTab(TransactionType.EXPENSE)
            }
            
        } catch (e: Exception) {
             e.printStackTrace()
             // Simple error handling
        }
    }
    
    // UI Helpers

    private fun applyPendingWalletSelection() {
        if (allWallets.isEmpty()) return

        val sourceWalletId = pendingWalletIdForEdit
        if (sourceWalletId != null) {
            val sourceIndex = allWallets.indexOfFirst { it.id == sourceWalletId }
            if (sourceIndex >= 0) {
                spinnerWallet.setSelection(sourceIndex)
                selectedWalletId = allWallets[sourceIndex].id
            } else {
                // Keep original id even if wallet is archived and hidden from spinner.
                selectedWalletId = sourceWalletId
            }
            pendingWalletIdForEdit = null
        }

        val targetWalletId = pendingTargetWalletIdForEdit
        if (targetWalletId != null) {
            val targetIndex = allWallets.indexOfFirst { it.id == targetWalletId }
            if (targetIndex >= 0) {
                spinnerTargetWallet.setSelection(targetIndex)
                selectedTargetWalletId = allWallets[targetIndex].id
            } else {
                selectedTargetWalletId = targetWalletId
            }
            pendingTargetWalletIdForEdit = null
        }
    }
    
    private fun switchTab(type: TransactionType) {
        currentType = type
        
        val colorSecondary = ContextCompat.getColor(this, R.color.text_secondary)
        val colorWhite = ContextCompat.getColor(this, R.color.text_white)

        // Reset
        tabExpense.setBackgroundResource(R.drawable.bg_tab_inactive_left)
        tabIncome.setBackgroundResource(R.drawable.bg_tab_inactive_center)
        tabDebt.setBackgroundResource(R.drawable.bg_tab_inactive_center)
        tabTransfer.setBackgroundResource(R.drawable.bg_tab_inactive_right)
        
        tabExpense.setTextColor(colorSecondary)
        tabIncome.setTextColor(colorSecondary)
        tabDebt.setTextColor(colorSecondary)
        tabTransfer.setTextColor(colorSecondary)
        
        // Hide Extra UI
        findViewById<View>(R.id.layoutContact).visibility = View.GONE
        findViewById<View>(R.id.dividerContact).visibility = View.GONE
        layoutTargetWallet.visibility = View.GONE
        lblSourceWallet.text = "Nguồn tiền:"

        // Active
        when (type) {
             TransactionType.EXPENSE -> {
                tabExpense.setBackgroundResource(R.drawable.bg_tab_active_expense)
                tabExpense.setTextColor(colorWhite)
             }
             TransactionType.INCOME -> {
                tabIncome.setBackgroundResource(R.drawable.bg_tab_active_income)
                tabIncome.setTextColor(colorWhite)
                lblSourceWallet.text = "Ví nhận:"
             }
             TransactionType.LOAN_GIVE, TransactionType.LOAN_TAKE -> {
                tabDebt.setBackgroundResource(R.drawable.bg_tab_active_debt)
                tabDebt.setTextColor(colorWhite)
                findViewById<View>(R.id.layoutContact).visibility = View.VISIBLE
                findViewById<View>(R.id.dividerContact).visibility = View.VISIBLE
             }
             TransactionType.TRANSFER -> {
                tabTransfer.setBackgroundResource(R.drawable.bg_tab_active_debt) // Use Debt color (Blue) or Create new
                tabTransfer.setTextColor(colorWhite)
                layoutTargetWallet.visibility = View.VISIBLE
                lblSourceWallet.text = "Từ ví:"
             }
        }
        
        selectedCategory = null
        categoryAdapter.setSelected(null)
        updateCategoryList()
    }
    
    private fun saveTransaction(isEditMode: Boolean, editId: Long) {
        try {
            val amount = com.nhattien.expensemanager.utils.CurrencyUtils.parseFromSeparator(edtAmount.text.toString())
            if (amount <= 0) {
                Toast.makeText(this, R.string.error_enter_amount, Toast.LENGTH_SHORT).show()
                return
            }
            
            // Validate Category if not Transfer
            if (currentType != TransactionType.TRANSFER && selectedCategory == null) {
                Toast.makeText(this, R.string.error_select_category, Toast.LENGTH_SHORT).show()
                return
            }
            
            // For Transfer, we can assign a dummy category or handle logic
            // Ideally Transfer doesn't need category, but DB schema requires categoryId.
            // We should have a System Category "Transfer" or similar.
            // Or allow null categoryId (requires Schema change).
            // Workaround: Find or Create a "Transfer" Category.
            // Or just pick the first category available if null? No.
            // Let's enforce selection or auto-select "Other".
            // Implementation Plan didn't specify Category for Transfer.
            // Let's search for a category named "Chuyển khoản" or create it?
            // Simple approach: Use selectedCategory if available, else require it? 
            // Transfers usually don't have categories.
            // But TransactionEntity has categoryId non-null.
            // I will default to ID 1 or find "Chuyển khoản".
            // Better: Auto-create "Transfers" category if missing.
            
            // Hack for now: If Transfer, use any category (e.g., first one) or user must select.
            // Let's force user to select a category even for Transfer (e.g. "Tiết kiệm", "Gửi tiền").
            // Or remove check?
            // Let's remove check if Transfer, and find a default category 
            
            val categoryId = if (currentType == TransactionType.TRANSFER) {
                 // Try to find "Chuyển khoản"
                 val transferCat = allCategories.find { it.name.contains("Chuyển") || it.name.contains("Transfer") }
                 transferCat?.id ?: (if(allCategories.isNotEmpty()) allCategories[0].id else 1L)
            } else {
                 selectedCategory!!.id
            }

            val finalType = if (currentType == TransactionType.TRANSFER) {
                TransactionType.TRANSFER
            } else {
                // Debt tab shows both "Đi vay" and "Cho vay", so type must follow selected category.
                selectedCategory?.type ?: currentType
            }
            
            // Validate Transfer Wallets
            if (currentType == TransactionType.TRANSFER) {
                if (selectedWalletId == selectedTargetWalletId) {
                     Toast.makeText(this, "Ví nguồn và ví đích phải khác nhau", Toast.LENGTH_SHORT).show()
                     return
                }
                if (selectedTargetWalletId == null) {
                     Toast.makeText(this, "Vui lòng chọn ví đích", Toast.LENGTH_SHORT).show()
                     return
                }
            }
            
            // Note logic
            var finalNote = edtNote.text.toString()
            val contactName = findViewById<EditText>(R.id.edtContact).text.toString()
            if (findViewById<View>(R.id.layoutContact).visibility == View.VISIBLE && contactName.isNotBlank()) {
                 finalNote = "Liên quan: $contactName. $finalNote"
            }
            
            val tagIds = selectedTags.map { it.id }

            if (isEditMode) {
                viewModel.updateTransaction(
                    id = editId,
                    amount = amount,
                    type = finalType,
                    categoryId = categoryId,
                    paymentMethod = "WALLET", // Deprecated string
                    note = finalNote,
                    date = selectedDateInMillis,
                    isRecurring = swipeRecurring.isChecked,
                    tagIds = tagIds,
                    walletId = selectedWalletId,
                    targetWalletId = if(currentType == TransactionType.TRANSFER) selectedTargetWalletId else null,
                    onSuccess = {
                        Toast.makeText(this, "Đã cập nhật giao dịch", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            } else {
                viewModel.addTransaction(
                    amount = amount,
                    type = finalType,
                    categoryId = categoryId,
                    paymentMethod = "WALLET", 
                    note = finalNote,
                    date = selectedDateInMillis,
                    isRecurring = swipeRecurring.isChecked,
                    tagIds = tagIds,
                    walletId = selectedWalletId,
                    targetWalletId = if(currentType == TransactionType.TRANSFER) selectedTargetWalletId else null,
                    onSuccess = {
                        Toast.makeText(this, R.string.msg_transaction_added, Toast.LENGTH_SHORT).show()
                        com.nhattien.expensemanager.widget.ExpenseWidgetProvider.updateAllWidgets(this)
                        finish()
                    }
                )
            }

        } catch (e: Exception) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Lỗi")
                .setMessage(e.toString())
                .setPositiveButton("OK", null)
                .show()
        }
    }
    
    private fun showTagSelectionDialog() {
        if (allTagsList.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Chưa có Tags")
                .setMessage("Bạn chưa có Tag nào.")
                .setPositiveButton("Tạo Tag") { _, _ ->
                     startActivity(Intent(this, ManageTagsActivity::class.java))
                }
                .setNegativeButton("Hủy", null)
                .show()
            return
        }

        val tagNames = allTagsList.map { it.name }.toTypedArray()
        val checkedItems = BooleanArray(allTagsList.size)
        allTagsList.forEachIndexed { index, tag ->
            if (selectedTags.any { it.id == tag.id }) {
                checkedItems[index] = true
            }
        }
        
        val tempSelectedTags = mutableListOf<TagEntity>()
        tempSelectedTags.addAll(selectedTags)

        AlertDialog.Builder(this)
            .setTitle("Chọn Tags")
            .setMultiChoiceItems(tagNames, checkedItems) { _, which, isChecked ->
                val tag = allTagsList[which]
                if (isChecked) if (tempSelectedTags.none { it.id == tag.id }) tempSelectedTags.add(tag)
                else tempSelectedTags.removeAll { it.id == tag.id }
            }
            .setPositiveButton("OK") { _, _ ->
                selectedTags.clear()
                selectedTags.addAll(tempSelectedTags)
                refreshTagChips()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun refreshTagChips() {
        chipGroupTags.removeAllViews()
        selectedTags.forEach { tag ->
             val chip = Chip(this)
             chip.text = tag.name
             chip.isCloseIconVisible = true
             chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(tag.color)
             chip.setTextColor(ContextCompat.getColor(this, R.color.text_white))
             chip.setOnCloseIconClickListener { 
                 selectedTags.remove(tag)
                 refreshTagChips()
             }
             chipGroupTags.addView(chip)
        }
    }
    
    private fun updateCategoryList() {
        if (currentType == TransactionType.TRANSFER) {
             categoryAdapter.submitList(emptyList()) // Hide categories or show specific?
             // Or show Expense categories?
             // Let's show All or Empty. Empty looks broken.
             // Let's show Expense categories as default
             categoryAdapter.submitList(allCategories.filter { it.type == TransactionType.EXPENSE })
        } else {
             val filtered = if (currentType == TransactionType.LOAN_GIVE || currentType == TransactionType.LOAN_TAKE) {
                allCategories.filter { it.type == TransactionType.LOAN_GIVE || it.type == TransactionType.LOAN_TAKE }
            } else {
                allCategories.filter { it.type == currentType }
            }
            categoryAdapter.submitList(filtered)
        }
    }
    
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
        
        btnCancel.setOnClickListener { dialog.dismiss() }
        
        btnAdd.setOnClickListener {
            val icon = edtIcon.text.toString().trim()
            val name = edtName.text.toString().trim()
            if (name.isNotEmpty() && icon.isNotEmpty()) {
                viewModel.addCategory(name, icon, if(currentType == TransactionType.TRANSFER) TransactionType.EXPENSE else currentType) {
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}

