package com.nhattien.expensemanager.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.domain.TransactionType
import com.nhattien.expensemanager.ui.add.AddTransactionActivity
import com.nhattien.expensemanager.ui.main.MainActivity
import com.nhattien.expensemanager.utils.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

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

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Use goAsync() to extend the BroadcastReceiver lifecycle for coroutine
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Use applicationContext to avoid ReceiverRestrictedContext errors
                val appContext = context.applicationContext
                val database = AppDatabase.getInstance(appContext)
                
                // Get all transactions for calculations
                val allTransactions = database.transactionDao().getAllTransactionsSync()
                
                // Calculate total balance
                var totalBalance = 0.0
                for (tx in allTransactions) {
                    totalBalance += if (tx.type == TransactionType.INCOME) tx.amount else -tx.amount
                }
                
                // Calculate today's expense
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val todayEnd = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                var todayExpense = 0.0
                for (tx in allTransactions) {
                    if (tx.type == TransactionType.EXPENSE && tx.date in todayStart..todayEnd) {
                        todayExpense += tx.amount
                    }
                }
                
                // Calculate this month's expense
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val monthEnd = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                
                var monthExpense = 0.0
                for (tx in allTransactions) {
                    if (tx.type == TransactionType.EXPENSE && tx.date in monthStart..monthEnd) {
                        monthExpense += tx.amount
                    }
                }
                
                // Create and configure RemoteViews
                val views = RemoteViews(context.packageName, R.layout.widget_expense)
                
                // Set text values
                views.setTextViewText(R.id.txtBalance, CurrencyUtils.toCurrency(totalBalance))
                views.setTextViewText(R.id.txtTodayExpense, "- ${CurrencyUtils.toCurrency(todayExpense)}")
                views.setTextViewText(R.id.txtMonthExpense, "- ${CurrencyUtils.toCurrency(monthExpense)}")
                
                // Click on widget root to open main app
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetRoot, mainPendingIntent)
                
                // Click on Quick Add button to open AddTransactionActivity
                val addIntent = Intent(context, AddTransactionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val addPendingIntent = PendingIntent.getActivity(
                    context, 1, addIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.btnQuickAdd, addPendingIntent)
                
                // Update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Show error state
                try {
                    val views = RemoteViews(context.packageName, R.layout.widget_expense)
                    views.setTextViewText(R.id.txtBalance, "Lỗi")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            } finally {
                // IMPORTANT: Signal that the async work is done
                pendingResult.finish()
            }
        }
    }

    companion object {
        /**
         * Call this method to update all widgets when data changes
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, ExpenseWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ExpenseWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            context.sendBroadcast(intent)
        }
    }
}
