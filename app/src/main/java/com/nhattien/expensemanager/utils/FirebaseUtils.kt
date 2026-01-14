package com.nhattien.expensemanager.utils

import android.content.Context
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirebaseUtils {

    suspend fun backupData(context: Context, uid: String): Pair<Boolean, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val transactions = db.transactionDao().getAllTransactionsSync()
                
                // Save to Firebase Realtime Database
                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("users").child(uid).child("transactions")
                
                ref.setValue(transactions).await()
                Pair(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(false, e.message ?: "Unknown error")
            }
        }
    }

    suspend fun restoreData(context: Context, uid: String): Pair<Boolean, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val database = FirebaseDatabase.getInstance()
                val ref = database.getReference("users").child(uid).child("transactions")
                
                val snapshot = ref.get().await()
                
                if (snapshot.exists()) {
                    val typeIndicator = object : com.google.firebase.database.GenericTypeIndicator<List<TransactionEntity>>() {}
                    val transactions = snapshot.getValue(typeIndicator)
                    
                    if (transactions != null) {
                        val db = AppDatabase.getInstance(context)
                        // Clear old data
                        db.transactionDao().deleteAll()
                        db.debtDao().deleteAll() 
                        
                        // Insert new data
                        db.transactionDao().insertAll(transactions)
                        Pair(true, null)
                    } else {
                        Pair(false, "Dữ liệu trên Cloud bị rỗng hoặc lỗi định dạng")
                    }
                } else {
                    Pair(false, "Chưa có dữ liệu nào trên Cloud")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(false, e.message ?: "Unknown error")
            }
        }
    }
}
