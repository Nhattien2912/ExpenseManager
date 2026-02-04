---
name: Notifications
description: Hiển thị thông báo nhắc nhở, cảnh báo chi tiêu.
---

# Android Notifications

## Tạo Notification Channel (Android 8+)

```kotlin
object NotificationHelper {
    
    const val CHANNEL_REMINDER = "reminder_channel"
    const val CHANNEL_BUDGET = "budget_channel"
    
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            
            // Kênh nhắc nhở
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER,
                "Nhắc nhở ghi chép",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nhắc nhở bạn ghi chép chi tiêu hàng ngày"
            }
            
            // Kênh cảnh báo ngân sách
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Cảnh báo ngân sách",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cảnh báo khi vượt ngân sách"
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(reminderChannel, budgetChannel)
            )
        }
    }
}
```

## Hiển thị Notification

```kotlin
fun showReminderNotification(context: Context) {
    val intent = Intent(context, AddTransactionActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )
    
    val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDER)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Đừng quên ghi chép!")
        .setContentText("Bạn đã ghi chép chi tiêu hôm nay chưa?")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .addAction(
            R.drawable.ic_add,
            "Thêm ngay",
            pendingIntent
        )
        .build()
    
    NotificationManagerCompat.from(context).notify(1001, notification)
}
```

## Cảnh báo vượt ngân sách

```kotlin
fun showBudgetWarning(context: Context, percentage: Int) {
    val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_BUDGET)
        .setSmallIcon(R.drawable.ic_warning)
        .setContentTitle("⚠️ Cảnh báo chi tiêu!")
        .setContentText("Bạn đã chi $percentage% ngân sách tháng này")
        .setStyle(NotificationCompat.BigTextStyle()
            .bigText("Bạn đã chi $percentage% ngân sách tháng này. Hãy cân nhắc giảm chi tiêu để không vượt quá giới hạn."))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    
    NotificationManagerCompat.from(context).notify(1002, notification)
}
```

## Scheduled Notification với AlarmManager

```kotlin
fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )
    
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }
    
    // Nếu đã qua giờ hôm nay, đặt cho ngày mai
    if (calendar.timeInMillis <= System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    
    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        showReminderNotification(context)
    }
}
```

## Permission (Android 13+)

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
        != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
    }
}
```
