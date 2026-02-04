---
name: App Widgets
description: Tạo widget màn hình chính hiển thị thông tin nhanh.
---

# Android App Widgets

## Widget Provider

```kotlin
class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Lấy dữ liệu
            val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            val balance = prefs.getFloat("balance", 0f)
            val todayExpense = prefs.getFloat("today_expense", 0f)
            
            // Tạo RemoteViews
            val views = RemoteViews(context.packageName, R.layout.widget_expense)
            views.setTextViewText(R.id.txtBalance, CurrencyUtils.toCurrency(balance.toDouble()))
            views.setTextViewText(R.id.txtTodayExpense, "- ${CurrencyUtils.toCurrency(todayExpense.toDouble())}")
            
            // Click để mở app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            
            // Click để thêm transaction
            val addIntent = Intent(context, AddTransactionActivity::class.java)
            val addPendingIntent = PendingIntent.getActivity(
                context, 1, addIntent, PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnQuickAdd, addPendingIntent)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
```

## Widget Layout (widget_expense.xml)

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widgetRoot"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="@drawable/widget_background">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Số dư"
        android:textColor="#888" />

    <TextView
        android:id="@+id/txtBalance"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="24sp"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/txtTodayExpense"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="#F44336" />

    <ImageButton
        android:id="@+id/btnQuickAdd"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:src="@drawable/ic_add"
        android:background="@drawable/circle_button" />

</LinearLayout>
```

## Widget Info (xml/widget_info.xml)

```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp"
    android:minHeight="110dp"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_expense"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:previewImage="@drawable/widget_preview" />
```

## AndroidManifest.xml

```xml
<receiver
    android:name=".widget.ExpenseWidgetProvider"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/widget_info" />
</receiver>
```

## Update Widget từ App

```kotlin
// Khi có transaction mới, update widget
fun updateWidgets(context: Context) {
    val intent = Intent(context, ExpenseWidgetProvider::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    }
    val ids = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, ExpenseWidgetProvider::class.java))
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    context.sendBroadcast(intent)
}
```
