package com.nhattien.expensemanager.utils

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.nhattien.expensemanager.R

/**
 * Helper class to show interactive tutorial/spotlight on UI elements
 */
object TutorialHelper {
    
    private const val PREF_NAME = "tutorial_prefs"
    private const val KEY_MAIN_TUTORIAL_SHOWN = "main_tutorial_shown"
    
    /**
     * Check if tutorial has been shown before
     */
    fun isTutorialShown(context: Context, key: String = KEY_MAIN_TUTORIAL_SHOWN): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key, false)
    }
    
    /**
     * Mark tutorial as shown
     */
    fun setTutorialShown(context: Context, key: String = KEY_MAIN_TUTORIAL_SHOWN, shown: Boolean = true) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, shown).apply()
    }
    
    /**
     * Reset all tutorials (for testing or from settings)
     */
    fun resetAllTutorials(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    /**
     * Create a spotlight target for a view with smooth animations
     */
    fun createTarget(
        view: View,
        title: String,
        description: String,
        outerCircleColor: Int = R.color.primary,
        targetCircleColor: Int = android.R.color.white
    ): TapTarget {
        return TapTarget.forView(view, title, description)
            // Colors
            .outerCircleColor(outerCircleColor)
            .outerCircleAlpha(0.92f)
            .targetCircleColor(targetCircleColor)
            
            // Text styling
            .titleTextSize(22)
            .titleTextColor(android.R.color.white)
            .descriptionTextSize(16)
            .descriptionTextColor(android.R.color.white)
            .textColor(android.R.color.white)
            
            // Visual effects
            .dimColor(android.R.color.black)
            .drawShadow(true)
            .cancelable(true)
            .tintTarget(true)
            .transparentTarget(true)
            .targetRadius(50)
    }
    
    /**
     * Show main screen tutorial sequence
     */
    fun showMainTutorial(
        activity: Activity,
        fabAdd: View,
        btnHome: View,
        btnCalendar: View,
        btnDebt: View,
        btnSettings: View,
        balanceCard: View?,
        onComplete: (() -> Unit)? = null
    ) {
        val targets = mutableListOf<TapTarget>()
        
        // 1. FAB Add button
        targets.add(
            createTarget(
                fabAdd,
                "➕ Thêm giao dịch",
                "Nhấn nút này để thêm thu/chi mới"
            )
        )
        
        // 2. Home button
        targets.add(
            createTarget(
                btnHome,
                "🏠 Trang chủ",
                "Xem tổng quan thu chi và giao dịch gần đây"
            )
        )
        
        // 3. Calendar button
        targets.add(
            createTarget(
                btnCalendar,
                "📅 Lịch",
                "Xem chi tiêu theo ngày trên lịch"
            )
        )
        
        // 4. Debt button
        targets.add(
            createTarget(
                btnDebt,
                "📒 Sổ nợ",
                "Quản lý các khoản vay và cho vay"
            )
        )
        
        // 5. Settings button
        targets.add(
            createTarget(
                btnSettings,
                "⚙️ Cài đặt",
                "Tùy chỉnh app, sao lưu dữ liệu, dark mode..."
            )
        )
        
        // 6. Balance card (if visible)
        balanceCard?.let {
            targets.add(
                createTarget(
                    it,
                    "💰 Số dư",
                    "Xem tổng số dư hiện tại của bạn. Nhấn vào icon con mắt để ẩn/hiện số tiền."
                )
            )
        }
        
        // Show sequence
        TapTargetSequence(activity)
            .targets(targets)
            .continueOnCancel(true)
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    setTutorialShown(activity)
                    onComplete?.invoke()
                }
                
                override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                    // Optional: track progress
                }
                
                override fun onSequenceCanceled(lastTarget: TapTarget?) {
                    // User cancelled, still mark as shown
                    setTutorialShown(activity)
                    onComplete?.invoke()
                }
            })
            .start()
    }
    
    /**
     * Show Add Transaction tutorial
     */
    fun showAddTransactionTutorial(
        activity: Activity,
        amountField: View,
        categoryGrid: View,
        noteField: View,
        dateField: View,
        saveButton: View,
        onComplete: (() -> Unit)? = null
    ) {
        val key = "add_transaction_tutorial_shown"
        
        if (isTutorialShown(activity, key)) {
            return
        }
        
        val targets = listOf(
            createTarget(amountField, "💵 Số tiền", "Nhập số tiền giao dịch"),
            createTarget(categoryGrid, "📂 Danh mục", "Chọn danh mục phù hợp"),
            createTarget(noteField, "📝 Ghi chú", "Thêm ghi chú để nhớ dễ hơn"),
            createTarget(dateField, "📅 Ngày", "Chọn ngày giao dịch"),
            createTarget(saveButton, "💾 Lưu", "Nhấn để lưu giao dịch")
        )
        
        TapTargetSequence(activity)
            .targets(targets)
            .continueOnCancel(true)
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    setTutorialShown(activity, key)
                    onComplete?.invoke()
                }
                override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {}
                override fun onSequenceCanceled(lastTarget: TapTarget?) {
                    setTutorialShown(activity, key)
                }
            })
            .start()
    }
}
