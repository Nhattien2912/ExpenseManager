package com.nhattien.expensemanager

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.nhattien.expensemanager.ui.login.LoginActivity
import kotlin.system.exitProcess

class ExpenseManagerApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            Log.d("ExpenseManager", "Firebase Initialized Successfully")
        } catch (e: Exception) {
            Log.e("ExpenseManager", "Firebase Init Failed", e)
        }
        
        // Setup Global Crash Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ExpenseManager", "Fatal Crash: ${throwable.message}", throwable)
            
            // Try to show a toast if possible (often fails in crash, but worth a try)
            // or launch a CrashReportActivity next time.
            
            // For now, simpler approach: Just log and fallback
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
