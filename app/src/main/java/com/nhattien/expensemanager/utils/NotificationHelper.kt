package com.nhattien.expensemanager.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.receiver.ReminderReceiver
import com.nhattien.expensemanager.ui.main.MainActivity
import kotlinx.coroutines.launch
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_REMINDER = "reminder_channel_v2" // Changed to reset settings
    const val CHANNEL_BUDGET = "budget_channel"
    const val CHANNEL_DEBT = "debt_channel"

    const val ID_REMINDER = 1001
    const val ID_BUDGET = 1002
    const val ID_DEBT_BASE = 2000 // Debt notifications will use ID = base + debtId

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Reminder Channel - High Importance for Heads-up
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                context.getString(R.string.channel_reminder_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminder_desc)
                enableVibration(true)
            }

            // Budget Channel
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                context.getString(R.string.channel_budget_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_budget_desc)
                enableVibration(true)
            }

            // Debt Channel
            val debtChannel = NotificationChannel(
                CHANNEL_DEBT,
                context.getString(R.string.channel_debt_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_debt_desc)
            }

            notificationManager.createNotificationChannels(
                listOf(reminderChannel, budgetChannel, debtChannel)
            )
        }
    }

    fun showReminderNotification(context: Context) {
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Ensure channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager.getNotificationChannel(CHANNEL_REMINDER)
            if (channel == null) {
                createChannels(context)
            }
        }
        
        val title = context.getString(R.string.notif_reminder_title)
        val message = context.getString(R.string.notif_reminder_text)
        
        // Save to database for in-app notification history
        saveNotificationToDb(context, "reminder", title, message)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, ID_REMINDER, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX) 
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ID_REMINDER, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showBudgetWarning(context: Context, percentage: Int) {
        val title = context.getString(R.string.notif_budget_title)
        val message = context.getString(R.string.notif_budget_text, percentage)
        
        // Save to database
        saveNotificationToDb(context, "budget", title, message)
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.notif_budget_text_long, percentage)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(ID_BUDGET, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    private fun saveNotificationToDb(context: Context, type: String, title: String, message: String) {
        val db = com.nhattien.expensemanager.data.database.AppDatabase.getInstance(context)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.notificationDao().insert(
                com.nhattien.expensemanager.data.entity.NotificationEntity(
                    type = type,
                    title = title,
                    message = message
                )
            )
        }
    }

    fun showDebtReminder(context: Context, debtId: Long, name: String, amount: String) {
        val title = "Nhắc nợ"
        val message = "$name - $amount"
        
        // Save to database for in-app notification history
        saveNotificationToDb(context, "debt", title, message)
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_DEBT)
            .setSmallIcon(R.drawable.ic_debt)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        try {
            NotificationManagerCompat.from(context).notify((ID_DEBT_BASE + debtId).toInt(), notification)
        } catch (e: SecurityException) {
             e.printStackTrace()
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("HOUR", hour)
            putExtra("MINUTE", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, ID_REMINDER, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Nếu đã qua giờ hôm nay, đặt cho ngày mai
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Android 12+ requires permission check for exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Fallback to inexact alarm if permission not granted
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                // Save prefs anyway
                context.getSharedPreferences("expense_manager", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("REMINDER_HOUR", hour)
                    .putInt("REMINDER_MINUTE", minute)
                    .apply()
                return
            }
        }
        
        // Use setExactAndAllowWhileIdle for reliable Doze mode delivery
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        
        // Lưu giờ vào prefs để có thể lên lịch lại
        context.getSharedPreferences("expense_manager", Context.MODE_PRIVATE)
            .edit()
            .putInt("REMINDER_HOUR", hour)
            .putInt("REMINDER_MINUTE", minute)
            .apply()
    }
    
    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ID_REMINDER, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
