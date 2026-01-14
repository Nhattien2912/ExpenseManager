package com.nhattien.expensemanager.utils

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

object BackupUtils {

    private val gson = Gson()

    suspend fun exportData(context: Context): File? {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val transactions = db.transactionDao().getAllTransactionsSync() // Need to ensure DAO has synchronous or suspend list getter
                
                // If DAO only returns Flow, we might need a different approach or add a suspend function to DAO.
                // Assuming we can get list. If not, we fix DAO.
                
                val json = gson.toJson(transactions)
                
                val file = File(context.cacheDir, "expense_manager_backup.json")
                val writer = FileWriter(file)
                writer.write(json)
                writer.close()
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun importData(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.bufferedReader().use { it?.readText() } ?: return@withContext false
                
                val type = object : TypeToken<List<TransactionEntity>>() {}.type
                val transactions: List<TransactionEntity> = gson.fromJson(json, type)
                
                if (transactions.isNotEmpty()) {
                    val db = AppDatabase.getInstance(context)
                    db.transactionDao().deleteAll() // Need to ensure DAO has this
                    db.transactionDao().insertAll(transactions) // Need to ensure DAO has this
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
