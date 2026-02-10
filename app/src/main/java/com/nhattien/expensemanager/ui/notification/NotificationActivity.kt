package com.nhattien.expensemanager.ui.notification

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.databinding.ActivityNotificationsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationAdapter
    private val notificationDao by lazy { AppDatabase.getInstance(this).notificationDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadNotifications()
        
        // Mark all as read when opening
        lifecycleScope.launch(Dispatchers.IO) {
            notificationDao.markAllAsRead()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { notification ->
            // Handle click if needed
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            notificationDao.getAllNotifications().collectLatest { notifications ->
                if (notifications.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvNotifications.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvNotifications.visibility = View.VISIBLE
                    adapter.submitList(notifications)
                }
            }
        }
    }
}
