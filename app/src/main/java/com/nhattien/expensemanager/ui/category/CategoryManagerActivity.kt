package com.nhattien.expensemanager.ui.category

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.CategoryEntity
import com.nhattien.expensemanager.data.repository.CategoryRepository
import com.nhattien.expensemanager.domain.TransactionType
import com.nhattien.expensemanager.ui.adapter.CategoryAdapter
import com.nhattien.expensemanager.viewmodel.CategoryManagerViewModel
import com.nhattien.expensemanager.viewmodel.CategoryManagerViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.RecyclerView

class CategoryManagerActivity : AppCompatActivity() {

    private lateinit var viewModel: CategoryManagerViewModel
    private lateinit var adapter: CategoryAdapter
    private var allCategories: List<CategoryEntity> = emptyList()
    private var currentType = TransactionType.EXPENSE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_manager)

        val db = AppDatabase.getInstance(application)
        val repository = CategoryRepository(db.categoryDao())
        val factory = CategoryManagerViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[CategoryManagerViewModel::class.java]

        initViews()
        observeData()
    }

    private fun initViews() {
        val btnClose = findViewById<android.view.View>(R.id.btnClose)
        val tabExpense = findViewById<TextView>(R.id.tabExpense)
        val tabIncome = findViewById<TextView>(R.id.tabIncome)
        val rvCategories = findViewById<RecyclerView>(R.id.rvCategories)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        btnClose.setOnClickListener { finish() }

        adapter = CategoryAdapter(
            onCategoryClick = { category ->
                // On Click -> Show Details or Delete?
                // Show Dialog to Delete if not default
                if (!category.isDefault) {
                    showDeleteDialog(category)
                } else {
                    Toast.makeText(this, getString(R.string.msg_error_delete_default), Toast.LENGTH_SHORT).show()
                }
            },
            onAddCategoryClick = {
                showAddCategoryDialog()
            }
        )
        
        rvCategories.layoutManager = GridLayoutManager(this, 4)
        rvCategories.adapter = adapter

        // Tab Logic
        tabExpense.setOnClickListener {
            currentType = TransactionType.EXPENSE
            updateTabs(tabExpense, tabIncome)
            filterList()
        }

        tabIncome.setOnClickListener {
            currentType = TransactionType.INCOME
            updateTabs(tabIncome, tabExpense)
            filterList()
        }

        fabAdd.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun updateTabs(active: TextView, inactive: TextView) {
        active.setBackgroundResource(if (active.id == R.id.tabExpense) R.drawable.bg_tab_expense_active else R.drawable.bg_tab_income_active)
        active.setTextColor(ContextCompat.getColor(this, R.color.text_white))
        
        inactive.setBackgroundResource(0)
        inactive.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    private fun filterList() {
        val filtered = allCategories.filter { it.type == currentType }
        adapter.submitList(filtered)
    }

    private fun observeData() {
        viewModel.categories.observe(this) { list ->
            allCategories = list
            filterList()
        }
    }

    private fun showDeleteDialog(category: CategoryEntity) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_delete_category))
            .setMessage(getString(R.string.msg_confirm_delete_category, category.name))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                viewModel.deleteCategory(category) {
                     Toast.makeText(this, getString(R.string.msg_category_deleted), Toast.LENGTH_SHORT).show()
                     // Refresh
                     viewModel.reloadCategories { 
                         allCategories = it
                         filterList()
                     }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val edtName = dialogView.findViewById<EditText>(R.id.edtCategoryName)
        val edtIcon = dialogView.findViewById<EditText>(R.id.edtCategoryIcon)
        val btnAdd = dialogView.findViewById<android.view.View>(R.id.btnAdd)
        val btnCancel = dialogView.findViewById<android.view.View>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnAdd.setOnClickListener {
            val name = edtName.text.toString()
            val icon = edtIcon.text.toString().trim()
            
            if (name.isNotEmpty() && icon.isNotEmpty()) {
                viewModel.addCategory(name, icon, currentType) {
                    Toast.makeText(this, getString(R.string.msg_category_added), Toast.LENGTH_SHORT).show()
                    viewModel.reloadCategories { 
                         allCategories = it
                         filterList()
                     }
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, getString(R.string.error_enter_full_info), Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
