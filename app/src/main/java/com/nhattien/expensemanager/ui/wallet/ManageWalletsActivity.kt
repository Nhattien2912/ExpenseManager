package com.nhattien.expensemanager.ui.wallet

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.WalletEntity
import com.nhattien.expensemanager.ui.adapter.WalletAdapter
import com.nhattien.expensemanager.utils.CurrencyUtils
import com.nhattien.expensemanager.viewmodel.WalletViewModel
import com.nhattien.expensemanager.viewmodel.WalletViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ManageWalletsActivity : AppCompatActivity() {

    private lateinit var viewModel: WalletViewModel
    private lateinit var adapter: WalletAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_wallets)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val factory = WalletViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[WalletViewModel::class.java]

        val rvWallets = findViewById<RecyclerView>(R.id.rvWallets)
        adapter = WalletAdapter { wallet ->
            showEditWalletDialog(wallet)
        }
        rvWallets.layoutManager = LinearLayoutManager(this)
        rvWallets.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.allWallets, viewModel.walletBalances) { wallets, balances ->
                    wallets to balances
                }.collect { (wallets, balances) ->
                    adapter.balances = balances
                    adapter.submitList(wallets)
                }
            }
        }

        findViewById<ExtendedFloatingActionButton>(R.id.fabAddWallet).setOnClickListener {
            showAddWalletDialog()
        }
    }

    private fun showAddWalletDialog() {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val edtName = EditText(context).apply {
            hint = "Tên ví (VD: Momo, VCB)"
        }
        layout.addView(edtName)

        val edtBalance = EditText(context).apply {
            hint = "Số dư ban đầu"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            addTextChangedListener(CurrencyUtils.MoneyTextWatcher(this))
        }
        layout.addView(edtBalance)

        val edtIcon = EditText(context).apply {
            hint = "Biểu tượng (VD: W)"
            setText("W")
        }
        layout.addView(edtIcon)

        val btnColor = android.widget.Button(context).apply {
            text = "Chọn màu"
        }
        var selectedColor = android.graphics.Color.BLUE
        btnColor.setBackgroundColor(selectedColor)
        btnColor.setOnClickListener {
            showColorPickerDialog { color ->
                selectedColor = color
                btnColor.setBackgroundColor(color)
            }
        }
        layout.addView(btnColor)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Thêm ví mới")
            .setView(layout)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = edtName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập tên ví", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (viewModel.isWalletNameUsed(name)) {
                    Toast.makeText(context, "Tên ví đã tồn tại", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val amount = CurrencyUtils.parseFromSeparator(edtBalance.text.toString())
                if (amount < 0) {
                    Toast.makeText(context, "Số dư ban đầu không hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val icon = edtIcon.text.toString().trim()
                viewModel.insertWallet(
                    name = name,
                    initialBalance = amount,
                    icon = if (icon.isEmpty()) "W" else icon,
                    color = selectedColor
                )
                Toast.makeText(context, "Đã thêm ví", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditWalletDialog(wallet: WalletEntity) {
        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val edtName = EditText(context).apply {
            hint = "Tên ví"
            setText(wallet.name)
        }
        layout.addView(edtName)

        val edtBalance = EditText(context).apply {
            hint = "Số dư ban đầu"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(CurrencyUtils.formatWithSeparator(wallet.initialBalance))
            addTextChangedListener(CurrencyUtils.MoneyTextWatcher(this))
        }
        layout.addView(edtBalance)

        val edtIcon = EditText(context).apply {
            hint = "Biểu tượng"
            setText(wallet.icon)
        }
        layout.addView(edtIcon)

        val btnColor = android.widget.Button(context).apply {
            text = "Chọn màu"
        }
        var selectedColor = wallet.color
        btnColor.setBackgroundColor(selectedColor)
        btnColor.setOnClickListener {
            showColorPickerDialog { color ->
                selectedColor = color
                btnColor.setBackgroundColor(color)
            }
        }
        layout.addView(btnColor)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Sửa/Xóa ví")
            .setView(layout)
            .setNegativeButton("Hủy", null)
            .setNeutralButton("Xóa (Lưu trữ)") { _, _ ->
                if (wallet.id == 1L) {
                    Toast.makeText(context, "Không thể xóa ví mặc định", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.deleteWallet(wallet)
                    Toast.makeText(context, "Đã xóa ví", Toast.LENGTH_SHORT).show()
                }
            }
            .setPositiveButton("Lưu", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = edtName.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(context, "Vui lòng nhập tên ví", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (viewModel.isWalletNameUsed(name, excludeWalletId = wallet.id)) {
                    Toast.makeText(context, "Tên ví đã tồn tại", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val balance = CurrencyUtils.parseFromSeparator(edtBalance.text.toString())
                if (balance < 0) {
                    Toast.makeText(context, "Số dư ban đầu không hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val icon = edtIcon.text.toString().trim()
                viewModel.updateWallet(
                    wallet.copy(
                        name = name,
                        initialBalance = balance,
                        icon = if (icon.isEmpty()) "W" else icon,
                        color = selectedColor
                    )
                )
                Toast.makeText(context, "Đã cập nhật", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showColorPickerDialog(onColorSelected: (Int) -> Unit) {
        val colors = listOf(
            android.graphics.Color.parseColor("#F44336"),
            android.graphics.Color.parseColor("#2196F3"),
            android.graphics.Color.parseColor("#4CAF50"),
            android.graphics.Color.parseColor("#FF9800"),
            android.graphics.Color.parseColor("#9C27B0"),
            android.graphics.Color.parseColor("#009688")
        )

        val context = this
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Chọn màu")
            .setView(layout)
            .setNegativeButton("Hủy", null)
            .create()

        colors.forEach { color ->
            val view = android.view.View(context)
            val params = LinearLayout.LayoutParams(100, 100)
            params.setMargins(10, 0, 10, 0)
            view.layoutParams = params
            view.setBackgroundColor(color)
            view.setOnClickListener {
                onColorSelected(color)
                dialog.dismiss()
            }
            layout.addView(view)
        }

        dialog.show()
    }
}
