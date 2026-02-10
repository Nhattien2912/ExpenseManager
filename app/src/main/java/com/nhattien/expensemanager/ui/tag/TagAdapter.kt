package com.nhattien.expensemanager.ui.tag

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhattien.expensemanager.data.entity.TagEntity
import com.nhattien.expensemanager.databinding.ItemTagManagerBinding

class TagAdapter(
    private val onDeleteClick: (TagEntity) -> Unit
) : ListAdapter<TagEntity, TagAdapter.TagViewHolder>(TagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagManagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TagViewHolder(private val binding: ItemTagManagerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tag: TagEntity) {
            binding.tvTagName.text = tag.name
            binding.viewColor.setBackgroundColor(tag.color)
            binding.cardColor.strokeColor = tag.color
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(tag)
            }
        }
    }

    class TagDiffCallback : DiffUtil.ItemCallback<TagEntity>() {
        override fun areItemsTheSame(oldItem: TagEntity, newItem: TagEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TagEntity, newItem: TagEntity): Boolean {
            return oldItem == newItem
        }
    }
}
