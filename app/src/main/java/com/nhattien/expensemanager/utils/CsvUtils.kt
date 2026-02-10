package com.nhattien.expensemanager.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.nhattien.expensemanager.data.entity.TransactionWithCategory
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvUtils {

    fun exportTransactionsToCsv(context: Context, transactions: List<TransactionWithCategory>): Uri? {
        try {
            val fileName = "ExpenseManager_Export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val file = File(context.cacheDir, fileName)
            
            val writer = FileWriter(file)
            
            // Write Header
            writer.append("ID,Date,Type,Category,Amount,Payment Method,Note\n")
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            // Write Data
            for (item in transactions) {
                val t = item.transaction
                val dateStr = dateFormat.format(Date(t.date))
                val typeStr = when (t.type) {
                    com.nhattien.expensemanager.domain.TransactionType.INCOME -> "Income"
                    com.nhattien.expensemanager.domain.TransactionType.EXPENSE -> "Expense"
                    com.nhattien.expensemanager.domain.TransactionType.LOAN_GIVE -> "Lend"
                    com.nhattien.expensemanager.domain.TransactionType.LOAN_TAKE -> "Borrow"
                    com.nhattien.expensemanager.domain.TransactionType.TRANSFER -> "Transfer"
                }
                val categoryName = item.category.name
                val noteClean = t.note.replace(",", " ").replace("\n", " ") // Basic CSV escaping
                
                writer.append("${t.id},")
                writer.append("$dateStr,")
                writer.append("$typeStr,")
                writer.append("$categoryName,")
                writer.append("${t.amount},")
                writer.append("${t.paymentMethod},")
                writer.append("$noteClean\n")
            }
            
            writer.flush()
            writer.close()
            
            // Return URI using FileProvider
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
