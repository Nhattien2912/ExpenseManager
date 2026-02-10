package com.nhattien.expensemanager.ui.tag

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.data.database.AppDatabase
import com.nhattien.expensemanager.data.entity.TagEntity
import com.nhattien.expensemanager.databinding.ActivityManageTagsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageTagsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageTagsBinding
    private lateinit var adapter: TagAdapter
    private val tagDao by lazy { AppDatabase.getInstance(this).tagDao() }
    
    // Predefined colors
    private val colors = listOf(
        Color.parseColor("#F44336"), // Red
        Color.parseColor("#E91E63"), // Pink
        Color.parseColor("#9C27B0"), // Purple
        Color.parseColor("#673AB7"), // Deep Purple
        Color.parseColor("#3F51B5"), // Indigo
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#03A9F4"), // Light Blue
        Color.parseColor("#00BCD4"), // Cyan
        Color.parseColor("#009688"), // Teal
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#8BC34A"), // Light Green
        Color.parseColor("#CDDC39"), // Lime
        Color.parseColor("#FFEB3B"), // Yellow
        Color.parseColor("#FFC107"), // Amber
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#FF5722"), // Deep Orange
        Color.parseColor("#795548"), // Brown
        Color.parseColor("#9E9E9E"), // Grey
        Color.parseColor("#607D8B")  // Blue Grey
    )
    
    private var selectedColor = colors[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageTagsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupFab()
        loadTags()
    }

    private fun setupRecyclerView() {
        adapter = TagAdapter { tag ->
            showDeleteConfirmDialog(tag)
        }
        binding.rvTags.layoutManager = LinearLayoutManager(this)
        binding.rvTags.adapter = adapter
    }

    private fun loadTags() {
        lifecycleScope.launch {
            tagDao.getAllTags().collect { tags ->
                if (tags.isEmpty()) {
                    binding.layoutEmptyTags.visibility = View.VISIBLE
                    binding.rvTags.visibility = View.GONE
                } else {
                    binding.layoutEmptyTags.visibility = View.GONE
                    binding.rvTags.visibility = View.VISIBLE
                    adapter.submitList(tags)
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAddTag.setOnClickListener {
            showAddTagDialog()
        }
    }

    private fun showAddTagDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tag, null)
        val etTagName = dialogView.findViewById<TextInputEditText>(R.id.etTagName)
        val gridColors = dialogView.findViewById<GridLayout>(R.id.gridColors)
        val activeColorView = dialogView.findViewById<View>(R.id.viewSelectedColor)
        
        selectedColor = colors.random()
        activeColorView.backgroundTintList = android.content.res.ColorStateList.valueOf(selectedColor)

        // Setup Color Grid with circular swatches
        val density = resources.displayMetrics.density
        val sizeInDp = (40 * density).toInt()
        val marginInDp = (6 * density).toInt()
        
        colors.forEach { color ->
            val colorView = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = sizeInDp
                    height = sizeInDp
                    setMargins(marginInDp, marginInDp, marginInDp, marginInDp)
                }
                background = resources.getDrawable(R.drawable.bg_circle_color, theme)
                backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                setOnClickListener { 
                    selectedColor = color
                    activeColorView.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                }
            }
            gridColors.addView(colorView)
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("LƯU") { _, _ ->
                val name = etTagName.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveTag(name, selectedColor)
                } else {
                    Toast.makeText(this, "Vui lòng nhập tên Tag", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("HỦY", null)
            .show()
    }

    private fun saveTag(name: String, color: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            tagDao.insertTag(TagEntity(name = name, color = color))
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ManageTagsActivity, "Đã thêm Tag: $name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmDialog(tag: TagEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa Tag")
            .setMessage("Bạn có chắc muốn xóa tag '${tag.name}'? Các giao dịch liên quan sẽ không bị xóa, chỉ mất nhãn.")
            .setPositiveButton("XÓA") { _, _ ->
                deleteTag(tag)
            }
            .setNegativeButton("HỦY", null)
            .show()
    }

    private fun deleteTag(tag: TagEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            tagDao.deleteTag(tag)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ManageTagsActivity, "Đã xóa Tag", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
