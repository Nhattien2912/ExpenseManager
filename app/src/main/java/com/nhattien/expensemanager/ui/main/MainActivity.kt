package com.nhattien.expensemanager.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.repository.TransactionRepository
import com.nhattien.expensemanager.ui.adapter.ExpenseAdapter
import com.nhattien.expensemanager.viewmodel.AddTransactionViewModel
import com.nhattien.expensemanager.viewmodel.AddTransactionViewModelFactory
import com.nhattien.expensemanager.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ExpenseViewModel
    private lateinit var adapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = ExpenseAdapter()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        lifecycleScope.launch {
            viewModel.expenses.collect { list ->
                adapter.submitList(list)
            }
        }
    }
}
