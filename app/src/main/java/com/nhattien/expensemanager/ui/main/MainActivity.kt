package com.nhattien.expensemanager.ui.main

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.budget.BudgetFragment
import com.nhattien.expensemanager.ui.setting.SettingFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            loadFragment(MainFragment())
        }

        // Xử lý các nút điều hướng tùy chỉnh
        findViewById<View>(R.id.btnNavHome).setOnClickListener { loadFragment(MainFragment()) }
        findViewById<View>(R.id.btnNavCalendar).setOnClickListener { loadFragment(CalendarFragment()) }
        findViewById<View>(R.id.btnNavDebt).setOnClickListener { loadFragment(com.nhattien.expensemanager.ui.debt.DebtFragment()) }
        findViewById<View>(R.id.btnNavSettings).setOnClickListener { loadFragment(SettingFragment()) }

        // Fix lỗi văng app khi nhấn nút Cộng (+)
        findViewById<View>(R.id.fab_add).setOnClickListener {
            try {
                val intent = android.content.Intent(this, com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Lỗi: " + e.message, android.widget.Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        // Update Bottom Nav State based on Fragment Type
        when (fragment) {
            is MainFragment -> updateBottomNavState(R.id.btnNavHome)
            is CalendarFragment -> updateBottomNavState(R.id.btnNavCalendar)
            is com.nhattien.expensemanager.ui.debt.DebtFragment -> updateBottomNavState(R.id.btnNavDebt)
            is SettingFragment -> updateBottomNavState(R.id.btnNavSettings)
        }
    }

    private fun updateBottomNavState(selectedId: Int) {
        val navIds = listOf(R.id.btnNavHome, R.id.btnNavCalendar, R.id.btnNavDebt, R.id.btnNavSettings)
        val context = this
        
        navIds.forEach { id ->
            val view = findViewById<View>(id)
            val icon = view.findViewById<ImageView>(view.resources.getIdentifier("imageView", "id", packageName) 
                ?: (view as android.view.ViewGroup).getChildAt(0).id) // Fallback or assume consistency 
            // Layout structure is LinearLayout -> ImageView, TextView.
            // Let's rely on child index or known IDs if they were set. 
            // In layout XML: ImageView doesn't have ID, TextView doesn't have ID in some items.
            // Let's iterate children.
            
            val container = view as? android.widget.LinearLayout ?: return@forEach
            val img = container.getChildAt(0) as? ImageView
            val txt = container.getChildAt(1) as? TextView

            if (id == selectedId) {
                img?.setColorFilter(android.graphics.Color.parseColor("#2196F3"))
                txt?.setTextColor(android.graphics.Color.parseColor("#2196F3"))
                txt?.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                img?.setColorFilter(android.graphics.Color.parseColor("#757575"))
                txt?.setTextColor(android.graphics.Color.parseColor("#757575"))
                txt?.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }
}
