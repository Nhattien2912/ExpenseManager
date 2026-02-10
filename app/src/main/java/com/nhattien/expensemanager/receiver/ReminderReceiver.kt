package com.nhattien.expensemanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nhattien.expensemanager.utils.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Hiển thị thông báo
        NotificationHelper.showReminderNotification(context)
        
        // Lên lịch lại cho ngày hôm sau
        // Vì setExactAndAllowWhileIdle chỉ chạy 1 lần, cần reschedule
        val prefs = context.getSharedPreferences("expense_manager", Context.MODE_PRIVATE)
        val hour = intent.getIntExtra("HOUR", prefs.getInt("REMINDER_HOUR", 20))
        val minute = intent.getIntExtra("MINUTE", prefs.getInt("REMINDER_MINUTE", 0))
        
        // Chỉ reschedule nếu user vẫn bật thông báo
        val isReminderEnabled = prefs.getBoolean("KEY_DAILY_REMINDER", true)
        if (isReminderEnabled) {
            NotificationHelper.scheduleDailyReminder(context, hour, minute)
        }
    }
}
