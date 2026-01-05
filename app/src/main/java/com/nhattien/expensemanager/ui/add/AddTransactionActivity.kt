package com.nhattien.expensemanager.ui.add

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.repository.TransactionRepository
import com.nhattien.expensemanager.viewmodel.AddTransactionViewModel

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var viewModel: AddTransactionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        val db = AppDatabase.getInstance(this)
        val repository = TransactionRepository(db.transactionDao())

        viewModel = AddTransactionViewModel(repository)

        val edtAmount = findViewById<EditText>(R.id.edtAmount)
        val edtCategory = findViewById<EditText>(R.id.edtCategory)
        val edtNote = findViewById<EditText>(R.id.edtNote)
        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val amount = edtAmount.text.toString().toDoubleOrNull() ?: return@setOnClickListener
            val category = edtCategory.text.toString()
            val note = edtNote.text.toString()

            viewModel.addTransaction(
                amount = amount,
                type = "EXPENSE",
                category = category,
                note = note
            )
        }
    }
}