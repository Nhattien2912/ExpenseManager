package com.nhattien.expensemanager.ui.recurring

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.adapter.RecurringTransactionAdapter
import com.nhattien.expensemanager.viewmodel.ManageRecurringViewModel

class ManageRecurringActivity : AppCompatActivity() {

    private val viewModel: ManageRecurringViewModel by viewModels()
    private lateinit var adapter: RecurringTransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_recurring)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val rvRecurring = findViewById<RecyclerView>(R.id.rvRecurring)
        val layoutEmpty = findViewById<View>(R.id.layoutEmpty)

        adapter = RecurringTransactionAdapter(
            onDeleteClick = { transaction ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa lịch định kỳ")
                    .setMessage("Bạn có chắc muốn xóa lịch định kỳ này không?")
                    .setPositiveButton("Xóa") { _, _ ->
                        viewModel.deleteRecurring(transaction)
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            },
            onStatusChange = { id, isActive ->
                viewModel.updateStatus(id, isActive)
            }
        )

        rvRecurring.layoutManager = LinearLayoutManager(this)
        rvRecurring.adapter = adapter

        viewModel.recurringList.observe(this) { list ->
            adapter.submitList(list)
            if (list.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                rvRecurring.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                rvRecurring.visibility = View.VISIBLE
            }
        }
    }
}
