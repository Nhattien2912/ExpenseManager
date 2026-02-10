package com.nhattien.expensemanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nhattien.expensemanager.utils.NotificationHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("expense_manager", Context.MODE_PRIVATE)
            val isReminderEnabled = prefs.getBoolean("KEY_DAILY_REMINDER_ENABLED", false)

            if (isReminderEnabled) {
                val hour = prefs.getInt("KEY_REMINDER_HOUR", 20)
                val minute = prefs.getInt("KEY_REMINDER_MINUTE", 0)
                NotificationHelper.scheduleDailyReminder(context, hour, minute)
            }
        }
    }
}
