package com.nhattien.expensemanager.utils

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirebaseUtils {

    private val gson = Gson()
    private const val BACKUP_NODE = "backups"

    suspend fun backupData(context: Context, userId: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val transactions = db.transactionDao().getAllTransactionsSync()
                val categories = db.categoryDao().getAllCategories()

                val backupData = BackupData(
                    transactions = transactions,
                    categories = categories
                )

                val json = gson.toJson(backupData)

                val ref = FirebaseDatabase.getInstance()
                    .getReference(BACKUP_NODE)
                    .child(userId)

                ref.setValue(json).await()

                Pair(true, "Backup thành công!")
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(false, e.message ?: "Lỗi không xác định")
            }
        }
    }

    suspend fun restoreData(context: Context, userId: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val ref = FirebaseDatabase.getInstance()
                    .getReference(BACKUP_NODE)
                    .child(userId)

                val snapshot = ref.get().await()
                val json = snapshot.getValue(String::class.java)

                if (json.isNullOrEmpty()) {
                    return@withContext Pair(false, "Không có dữ liệu backup")
                }

                val backupData = gson.fromJson(json, BackupData::class.java)

                val db = AppDatabase.getInstance(context)

                // Clear existing data
                db.transactionDao().deleteAll()
                db.categoryDao().deleteAll()

                // Restore categories first (since transactions depend on them)
                if (backupData.categories.isNotEmpty()) {
                    db.categoryDao().insertAll(backupData.categories)
                }

                // Restore transactions
                if (backupData.transactions.isNotEmpty()) {
                    db.transactionDao().insertAll(backupData.transactions)
                }

                Pair(true, "Restore thành công!")
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(false, e.message ?: "Lỗi không xác định")
            }
        }
    }
}
