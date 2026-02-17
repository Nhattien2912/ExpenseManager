package com.nhattien.expensemanager.ui.planned

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.PlannedExpenseEntity
import com.nhattien.expensemanager.ui.adapter.PlannedExpenseAdapter
import com.nhattien.expensemanager.ui.wallet.ManageWalletsActivity
import com.nhattien.expensemanager.utils.CurrencyUtils
import com.nhattien.expensemanager.viewmodel.PlannedExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlannedExpenseActivity : AppCompatActivity() {

    private val viewModel: PlannedExpenseViewModel by viewModels()
    private lateinit var adapter: PlannedExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planned_expense)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = PlannedExpenseAdapter(
            onToggle = { item ->
                if (!item.isCompleted) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Xác nhận chi tiêu")
                        .setMessage("Tạo giao dịch '${item.title}' với số tiền ${CurrencyUtils.toCurrency(item.amount)}?")
                        .setPositiveButton("Xác nhận") { _, _ -> viewModel.toggleComplete(item) }
                        .setNegativeButton("Hủy", null)
                        .show()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Hoàn tác")
                        .setMessage("Bỏ đánh dấu '${item.title}'? Giao dịch đã tạo sẽ bị xóa.")
                        .setPositiveButton("Hoàn tác") { _, _ -> viewModel.toggleComplete(item) }
                        .setNegativeButton("Hủy", null)
                        .show()
                }
            },
            onDelete = { item ->
                val extra = if (item.isCompleted) "\nCảnh báo: giao dịch đã tạo cũng sẽ bị xóa." else ""
                MaterialAlertDialogBuilder(this)
                    .setTitle("Xóa mục")
                    .setMessage("Xóa '${item.title}' - ${CurrencyUtils.toCurrency(item.amount)}?$extra")
                    .setPositiveButton("Xóa") { _, _ -> viewModel.deleteItem(item) }
                    .setNegativeButton("Hủy", null)
                    .show()
            },
            onEdit = { item ->
                showEditItemDialog(item)
            }
        )

        val rv = findViewById<RecyclerView>(R.id.rvPlannedExpenses)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun setupFab() {
        findViewById<ExtendedFloatingActionButton>(R.id.fabAddPlan).setOnClickListener {
            val currentGroup = viewModel.currentGroup.value
            if (currentGroup == null) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Chưa chọn nhóm")
                    .setMessage("Bạn cần tạo hoặc chọn nhóm trước khi thêm mục dự tính.")
                    .setPositiveButton("Tạo nhóm") { _, _ -> showCreateGroupDialog() }
                    .setNegativeButton("Đóng", null)
                    .show()
            } else {
                showAddItemDialog()
            }
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allGroups.collect { groups ->
                        updateChips(groups)
                    }
                }

                launch {
                    viewModel.currentItems.collect { items ->
                        adapter.submitList(items)
                        val layoutEmpty = findViewById<LinearLayout>(R.id.layoutEmpty)
                        val rv = findViewById<RecyclerView>(R.id.rvPlannedExpenses)

                        if (viewModel.currentGroup.value != null) {
                            layoutEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                            rv.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                            findViewById<TextView>(R.id.txtEmptyMessage).text =
                                "Chưa có mục nào. Bấm + để thêm chi tiêu dự tính."
                        } else {
                            layoutEmpty.visibility = View.VISIBLE
                            rv.visibility = View.GONE
                            findViewById<TextView>(R.id.txtEmptyMessage).text =
                                "Chọn một nhóm ở trên hoặc tạo nhóm mới để bắt đầu."
                        }
                    }
                }

                launch {
                    viewModel.groupTotal.collect { total ->
                        findViewById<TextView>(R.id.txtTotalPlanned).text = CurrencyUtils.toCurrency(total)
                        updateProgress()
                    }
                }

                launch {
                    viewModel.completedTotal.collect { completed ->
                        findViewById<TextView>(R.id.txtTotalCompleted).text = CurrencyUtils.toCurrency(completed)
                        updateProgress()
                    }
                }

                launch {
                    viewModel.currentGroup.collect { group ->
                        val cardSummary = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSummary)
                        if (group != null) {
                            cardSummary.visibility = View.VISIBLE
                            findViewById<TextView>(R.id.txtGroupName).text = "Nhóm: $group"
                        } else {
                            cardSummary.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.categoryMap.collect { map ->
                        adapter.categoryMap = map
                        adapter.notifyDataSetChanged()
                    }
                }

                launch {
                    viewModel.walletMap.collect { map ->
                        adapter.walletMap = map
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun updateProgress() {
        val total = viewModel.groupTotal.value
        val completed = viewModel.completedTotal.value
        val progress = if (total > 0) ((completed / total) * 100).toInt() else 0
        findViewById<LinearProgressIndicator>(R.id.progressPlanned).setProgressCompat(progress, true)

        val items = viewModel.currentItems.value
        val completedCount = items.count { it.isCompleted }
        val totalCount = items.size
        val txtCount = findViewById<TextView>(R.id.txtItemCount)
        if (totalCount > 0) {
            txtCount.text = "$completedCount/$totalCount xong"
            txtCount.visibility = View.VISIBLE
        } else {
            txtCount.visibility = View.GONE
        }
    }

    private fun updateChips(groups: List<String>) {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupPlans)
        chipGroup.removeAllViews()

        val addChip = Chip(this).apply {
            text = "+ Nhóm mới"
            isCheckable = false
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.primary, theme))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { showCreateGroupDialog() }
        }
        chipGroup.addView(addChip)

        groups.forEach { group ->
            val chip = Chip(this).apply {
                text = group
                isCheckable = true
                isChecked = group == viewModel.currentGroup.value
                setOnClickListener {
                    viewModel.selectGroup(group)
                    updateChips(groups)
                }
                setOnLongClickListener {
                    MaterialAlertDialogBuilder(this@PlannedExpenseActivity)
                        .setTitle("Xóa nhóm '$group'?")
                        .setMessage("Tất cả mục trong nhóm sẽ bị xóa. Giao dịch đã tạo cũng sẽ bị xóa.")
                        .setPositiveButton("Xóa") { _, _ -> viewModel.deleteGroup(group) }
                        .setNegativeButton("Hủy", null)
                        .show()
                    true
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun showCreateGroupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val edtGroupName = dialogView.findViewById<TextInputEditText>(R.id.edtGroupName)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Tạo nhóm dự tính")
            .setView(dialogView)
            .setPositiveButton("Tạo", null)
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = edtGroupName.text.toString().trim().replace(Regex("\\s+"), " ")
                if (name.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val existed = viewModel.allGroups.value.any { it.equals(name, ignoreCase = true) }
                if (existed) {
                    Toast.makeText(this, "Nhóm đã tồn tại", Toast.LENGTH_SHORT).show()
                }

                viewModel.createGroup(name)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showAddItemDialog() {
        showItemDialog(existingItem = null)
    }

    private fun showEditItemDialog(item: PlannedExpenseEntity) {
        showItemDialog(existingItem = item)
    }

    private fun showItemDialog(existingItem: PlannedExpenseEntity?) {
        val view = layoutInflater.inflate(R.layout.dialog_add_planned_item, null)

        val edtAmount = view.findViewById<TextInputEditText>(R.id.edtPlanAmount)
        val edtNote = view.findViewById<TextInputEditText>(R.id.edtPlanNote)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerWallet = view.findViewById<Spinner>(R.id.spinnerWallet)
        val btnDueDate = view.findViewById<Button>(R.id.btnDueDate)

        edtAmount.addTextChangedListener(CurrencyUtils.MoneyTextWatcher(edtAmount))

        val categories = viewModel.expenseCategories.value
        if (categories.isEmpty()) {
            Toast.makeText(this, "Chưa có danh mục chi tiêu. Hãy thêm danh mục trước.", Toast.LENGTH_SHORT).show()
            return
        }

        val wallets = viewModel.wallets.value
        if (wallets.isEmpty()) {
            Toast.makeText(this, "Chưa có ví. Hãy tạo ví trước khi thêm kế hoạch.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ManageWalletsActivity::class.java))
            return
        }

        spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories.map { "${it.icon} ${it.name}" }
        )

        spinnerWallet.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            wallets.map { "${it.icon} ${it.name}" }
        )

        var selectedDueDate = existingItem?.dueDate ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        btnDueDate.text = "Ngày: ${sdf.format(selectedDueDate)}"

        if (existingItem != null) {
            edtAmount.setText(CurrencyUtils.formatWithSeparator(existingItem.amount))
            edtNote.setText(existingItem.note)

            val categoryIndex = categories.indexOfFirst { it.id == existingItem.categoryId }
            if (categoryIndex >= 0) spinnerCategory.setSelection(categoryIndex)

            val walletIndex = wallets.indexOfFirst { it.id == existingItem.walletId }
            if (walletIndex >= 0) spinnerWallet.setSelection(walletIndex)
        }

        btnDueDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDueDate }
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                selectedDueDate = cal.timeInMillis
                btnDueDate.text = "Ngày: ${sdf.format(selectedDueDate)}"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        val isEdit = existingItem != null
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) "Sửa mục chi tiêu" else "Thêm mục chi tiêu")
            .setView(view)
            .setPositiveButton(if (isEdit) "Lưu" else "Thêm", null)
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amountText = edtAmount.text.toString().trim()
                val note = edtNote.text.toString().trim()

                if (amountText.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val amount = CurrencyUtils.parseFromSeparator(amountText)
                if (amount <= 0) {
                    Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val categoryPos = spinnerCategory.selectedItemPosition
                val walletPos = spinnerWallet.selectedItemPosition
                if (categoryPos !in categories.indices || walletPos !in wallets.indices) {
                    Toast.makeText(this, "Dữ liệu danh mục hoặc ví không hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val categoryId = categories[categoryPos].id
                val walletId = wallets[walletPos].id
                val finalTitle = categories[categoryPos].name

                if (isEdit) {
                    val itemToUpdate = existingItem ?: return@setOnClickListener
                    viewModel.updateItem(
                        item = itemToUpdate,
                        title = finalTitle,
                        amount = amount,
                        categoryId = categoryId,
                        walletId = walletId,
                        note = note,
                        dueDate = selectedDueDate
                    )
                    Toast.makeText(this, "Đã cập nhật '$finalTitle'", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addItem(finalTitle, amount, categoryId, walletId, note, selectedDueDate)
                    Toast.makeText(this, "Đã thêm '$finalTitle'", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
