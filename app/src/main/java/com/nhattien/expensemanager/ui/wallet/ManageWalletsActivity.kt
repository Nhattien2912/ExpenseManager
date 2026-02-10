package com.nhattien.expensemanager.ui.wallet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.WalletEntity
import com.nhattien.expensemanager.ui.adapter.WalletAdapter
import com.nhattien.expensemanager.viewmodel.WalletViewModel
import com.nhattien.expensemanager.viewmodel.WalletViewModelFactory
import kotlinx.coroutines.launch

class ManageWalletsActivity : AppCompatActivity() {

    private lateinit var viewModel: WalletViewModel
    private lateinit var adapter: WalletAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_wallets)

        // Setup Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Setup ViewModel
        val factory = WalletViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[WalletViewModel::class.java]

        // Setup RecyclerView
        val rvWallets = findViewById<RecyclerView>(R.id.rvWallets)
        adapter = WalletAdapter { wallet ->
            showEditWalletDialog(wallet)
        }
        rvWallets.layoutManager = LinearLayoutManager(this)
        rvWallets.adapter = adapter

        // Observe Data
        lifecycleScope.launch {
            viewModel.allWallets.collect { wallets ->
                adapter.submitList(wallets)
            }
        }

        // Setup FAB
        findViewById<ExtendedFloatingActionButton>(R.id.fabAddWallet).setOnClickListener {
            showAddWalletDialog()
        }
    }

    private fun showAddWalletDialog() {
        val context = this
        val layout = android.widget.LinearLayout(context)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val edtName = EditText(context)
        edtName.hint = "Tên Ví (VD: Momo, VCB)"
        layout.addView(edtName)

        val edtBalance = EditText(context)
        edtBalance.hint = "Số dư ban đầu"
        edtBalance.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        edtBalance.addTextChangedListener(com.nhattien.expensemanager.utils.CurrencyUtils.MoneyTextWatcher(edtBalance))
        layout.addView(edtBalance)
        
        val edtIcon = EditText(context)
        edtIcon.hint = "Biểu tượng (VD: 💰)"
        edtIcon.setText("💰")
        layout.addView(edtIcon)
        
        // Color Picker Button
        val btnColor = android.widget.Button(context)
        btnColor.text = "Chọn Màu"
        var selectedColor = android.graphics.Color.BLUE
        btnColor.setBackgroundColor(selectedColor)
        btnColor.setOnClickListener {
            showColorPickerDialog { color ->
                selectedColor = color
                btnColor.setBackgroundColor(color)
            }
        }
        layout.addView(btnColor)

        AlertDialog.Builder(context)
            .setTitle("Thêm Ví Mới")
            .setView(layout)
            .setPositiveButton("Lưu") { _, _ ->
                val name = edtName.text.toString().trim()
                if (name.isNotEmpty()) {
                    val amount = com.nhattien.expensemanager.utils.CurrencyUtils.parseFromSeparator(edtBalance.text.toString())
                    val icon = edtIcon.text.toString().trim()
                    viewModel.insertWallet(name, amount, if(icon.isEmpty()) "💰" else icon, selectedColor)
                    Toast.makeText(context, "Đã thêm ví", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditWalletDialog(wallet: WalletEntity) {
        val context = this
        val layout = android.widget.LinearLayout(context)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val edtName = EditText(context)
        edtName.hint = "Tên Ví"
        edtName.setText(wallet.name)
        layout.addView(edtName)
        
        val edtIcon = EditText(context)
        edtIcon.hint = "Biểu tượng"
        edtIcon.setText(wallet.icon)
        layout.addView(edtIcon)

        // Color Picker Button
        val btnColor = android.widget.Button(context)
        btnColor.text = "Chọn Màu"
        var selectedColor = wallet.color
        btnColor.setBackgroundColor(selectedColor)
        btnColor.setOnClickListener {
            showColorPickerDialog { color ->
                selectedColor = color
                btnColor.setBackgroundColor(color)
            }
        }
        layout.addView(btnColor)

        AlertDialog.Builder(context)
            .setTitle("Sửa/Xóa Ví")
            .setView(layout)
            .setPositiveButton("Lưu") { _, _ ->
                val name = edtName.text.toString().trim()
                val icon = edtIcon.text.toString().trim()
                if (name.isNotEmpty()) {
                    val updated = wallet.copy(
                        name = name, 
                        icon = if(icon.isEmpty()) "💰" else icon,
                        color = selectedColor
                    )
                    viewModel.updateWallet(updated)
                    Toast.makeText(context, "Đã cập nhật", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .setNeutralButton("Xóa (Lưu trữ)") { _, _ ->
                if (wallet.id == 1L) {
                    Toast.makeText(context, "Không thể xóa Ví mặc định", Toast.LENGTH_SHORT).show()
                } else {
                    // Soft delete logic can be handled here or inside ViewModel delete
                    // For now, assuming delete removes it.
                    // If we want archive, we need isDeleted field.
                    viewModel.deleteWallet(wallet)
                    Toast.makeText(context, "Đã xóa ví", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
    
    private fun showColorPickerDialog(onColorSelected: (Int) -> Unit) {
        val colors = listOf(
            android.graphics.Color.parseColor("#F44336"), // Red
            android.graphics.Color.parseColor("#2196F3"), // Blue
            android.graphics.Color.parseColor("#4CAF50"), // Green
            android.graphics.Color.parseColor("#FF9800"), // Orange
            android.graphics.Color.parseColor("#9C27B0"), // Purple
            android.graphics.Color.parseColor("#009688")  // Teal
        )
        
        val context = this
        val layout = android.widget.LinearLayout(context)
        layout.orientation = android.widget.LinearLayout.HORIZONTAL
        layout.gravity = android.view.Gravity.CENTER
        layout.setPadding(40, 40, 40, 40)
        
        val dialog = AlertDialog.Builder(context)
            .setTitle("Chọn màu")
            .setView(layout)
            .setNegativeButton("Hủy", null)
            .create()

        colors.forEach { color ->
            val view = android.view.View(context)
            val params = android.widget.LinearLayout.LayoutParams(100, 100)
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
