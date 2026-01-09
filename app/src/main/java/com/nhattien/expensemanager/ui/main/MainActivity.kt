package com.nhattien.expensemanager.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.budget.BudgetFragment
import com.nhattien.expensemanager.ui.setting.SettingFragment
import com.nhattien.expensemanager.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Load màn hình chính đầu tiên
        if (savedInstanceState == null) {
            loadFragment(MainFragment())
        }

        val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        bottomAppBar.replaceMenu(R.menu.bottom_nav_menu)

        // Xử lý chuyển tab
        bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_calendar -> {
                    loadFragment(MainFragment())
                    true
                }
                R.id.nav_budget -> {
                    loadFragment(BudgetFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingFragment())
                    true
                }
                else -> false
            }
        }

        // Xử lý nút Cộng (+) - Chuyển sang Activity mới
        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            val intent = android.content.Intent(this, com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
            startActivity(intent)
        }

    } // <--- ĐÓNG HÀM onCreate Ở ĐÂY

    // ===> ĐƯA HÀM loadFragment RA NGOÀI NÀY <===
    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // (Tuỳ chọn) Thêm dòng này nếu muốn bấm Back để quay lại màn hình trước
            .commit()
    }

} // Đóng class MainActivity