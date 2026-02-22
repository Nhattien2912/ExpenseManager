package com.nhattien.expensemanager.ui.main

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.budget.BudgetFragment
import com.nhattien.expensemanager.ui.setting.SettingFragment
import com.nhattien.expensemanager.utils.TutorialHelper
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.nhattien.expensemanager.worker.RecurringTransactionWorker

class MainActivity : AppCompatActivity() {
    
    private lateinit var fabHelp: FloatingActionButton
    private var dX = 0f
    private var dY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        // Áp dụng Dark Mode trước khi setContentView
        val prefs = getSharedPreferences("expense_manager", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("KEY_DARK_MODE", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        // Init Notification Channels
        com.nhattien.expensemanager.utils.NotificationHelper.createChannels(this)
        
        // Check biometric lock
        if (com.nhattien.expensemanager.utils.BiometricHelper.isBiometricEnabled(this) &&
            com.nhattien.expensemanager.utils.BiometricHelper.canAuthenticate(this)) {
            // Only redirect if this is a fresh launch (not coming back from BiometricLockActivity)
            if (intent.getBooleanExtra("BIOMETRIC_PASSED", false).not()) {
                startActivity(android.content.Intent(this, com.nhattien.expensemanager.ui.lock.BiometricLockActivity::class.java))
                finish()
                return
            }
        }
        
        // Schedule Recurring Transactions Worker
        val syncWorkRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RecurringTransactionWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            loadFragment(MainFragment())
        }

        // Xử lý các nút điều hướng tùy chỉnh
        findViewById<View>(R.id.btnNavHome).setOnClickListener { loadFragment(MainFragment()) }
        findViewById<View>(R.id.btnNavCalendar).setOnClickListener { loadFragment(CalendarFragment()) }
        findViewById<View>(R.id.btnNavServices).setOnClickListener { loadFragment(ServicesFragment()) }
        findViewById<View>(R.id.btnNavSettings).setOnClickListener { loadFragment(SettingFragment()) }

        // Nút Thêm giao dịch (+)
        findViewById<View>(R.id.fab_add).setOnClickListener {
            try {
                val intent = android.content.Intent(this, com.nhattien.expensemanager.ui.add.AddTransactionActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Lỗi: " + e.message, android.widget.Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
        
        // Nút Hướng dẫn (💡) - Có thể kéo thả
        fabHelp = findViewById(R.id.fab_help)
        setupDraggableHelpButton()
        
        // Kiểm tra lần đầu mở app
        if (!TutorialHelper.isTutorialShown(this)) {
            window.decorView.post {
                showWelcomeDialog()
            }
        } else {
            // Ẩn nút help nếu đã xem tutorial
            fabHelp.visibility = View.GONE
        }
    }
    
    /**
     * Thiết lập nút help có thể kéo thả tự do
     */
    private fun setupDraggableHelpButton() {
        fabHelp.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Kiểm tra nếu không di chuyển nhiều thì coi như click
                    val moved = Math.abs(view.x - (event.rawX + dX)) > 10 || 
                                Math.abs(view.y - (event.rawY + dY)) > 10
                    if (!moved) {
                        showTutorial()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Hiển thị dialog chào mừng lần đầu
     */
    private fun showWelcomeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_welcome, null)
        val btnTutorial = dialogView.findViewById<View>(R.id.btnTutorial)
        val btnSkip = dialogView.findViewById<View>(R.id.btnSkip)
        
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        btnTutorial.setOnClickListener {
            dialog.dismiss()
            showTutorial()
        }
        
        btnSkip.setOnClickListener {
            dialog.dismiss()
            TutorialHelper.setTutorialShown(this)
            fabHelp.visibility = View.GONE
            android.widget.Toast.makeText(this, "Bạn có thể xem hướng dẫn trong Cài đặt", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        dialog.show()
    }
    
    /**
     * Hiển thị hướng dẫn sử dụng app
     */
    fun showTutorial() {
        try {
            val fabAdd = findViewById<View>(R.id.fab_add)
            val btnHome = findViewById<View>(R.id.btnNavHome)
            val btnCalendar = findViewById<View>(R.id.btnNavCalendar)
            val btnServices = findViewById<View>(R.id.btnNavServices)
            val btnSettings = findViewById<View>(R.id.btnNavSettings)
            
            if (fabAdd == null || btnHome == null || btnCalendar == null || btnServices == null || btnSettings == null) {
                android.widget.Toast.makeText(this, "Không thể hiển thị hướng dẫn", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            TutorialHelper.showMainTutorial(
                activity = this,
                fabAdd = fabAdd,
                btnHome = btnHome,
                btnCalendar = btnCalendar,
                btnDebt = btnServices,
                btnSettings = btnSettings,
                balanceCard = null,
                onComplete = {
                    // Ẩn nút help sau khi xem xong
                    fabHelp.visibility = View.GONE
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Lỗi hiển thị hướng dẫn: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        when (fragment) {
            is MainFragment -> updateBottomNavState(R.id.btnNavHome)
            is CalendarFragment -> updateBottomNavState(R.id.btnNavCalendar)
            is ServicesFragment -> updateBottomNavState(R.id.btnNavServices)
            is SettingFragment -> updateBottomNavState(R.id.btnNavSettings)
        }
    }

    private fun updateBottomNavState(selectedId: Int) {
        val navIds = listOf(R.id.btnNavHome, R.id.btnNavCalendar, R.id.btnNavServices, R.id.btnNavSettings)
        
        navIds.forEach { id ->
            val view = findViewById<View>(id)
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

