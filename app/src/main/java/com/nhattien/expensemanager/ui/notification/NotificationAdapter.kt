package com.nhattien.expensemanager.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.entity.NotificationEntity
import com.nhattien.expensemanager.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val onClick: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotificationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationEntity) {
            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message
            binding.tvTime.text = getRelativeTime(notification.timestamp)
            
            // Show unread indicator
            binding.viewUnread.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            
            // Set icon and color based on type
            when (notification.type) {
                "budget" -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_warning)
                    binding.viewIconBg.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                            binding.root.context.getColor(R.color.expense)
                        )
                    )
                    binding.ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.WHITE
                    )
                }
                "debt" -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_debt)
                    binding.viewIconBg.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                            binding.root.context.getColor(R.color.primary)
                        )
                    )
                }
                else -> {
                    binding.ivIcon.setImageResource(R.drawable.ic_notification)
                }
            }
            
            binding.root.setOnClickListener { onClick(notification) }
        }
        
        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Vừa xong"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} phút trước"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} giờ trước"
                diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} ngày trước"
                else -> {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
        override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity) =
            oldItem == newItem
    }
}
