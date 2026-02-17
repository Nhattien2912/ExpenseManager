package com.nhattien.expensemanager.ui.tag

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.Toast
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

    private val colors = listOf(
        Color.parseColor("#F44336"),
        Color.parseColor("#E91E63"),
        Color.parseColor("#9C27B0"),
        Color.parseColor("#673AB7"),
        Color.parseColor("#3F51B5"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#03A9F4"),
        Color.parseColor("#00BCD4"),
        Color.parseColor("#009688"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#8BC34A"),
        Color.parseColor("#CDDC39"),
        Color.parseColor("#FFEB3B"),
        Color.parseColor("#FFC107"),
        Color.parseColor("#FF9800"),
        Color.parseColor("#FF5722"),
        Color.parseColor("#795548"),
        Color.parseColor("#9E9E9E"),
        Color.parseColor("#607D8B")
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
        adapter = TagAdapter(
            onTagClick = { tag -> showEditTagDialog(tag) },
            onDeleteClick = { tag -> showDeleteConfirmDialog(tag) }
        )
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
        showTagEditorDialog(existingTag = null)
    }

    private fun showEditTagDialog(tag: TagEntity) {
        showTagEditorDialog(existingTag = tag)
    }

    private fun showTagEditorDialog(existingTag: TagEntity?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tag, null)
        val etTagName = dialogView.findViewById<TextInputEditText>(R.id.etTagName)
        val gridColors = dialogView.findViewById<GridLayout>(R.id.gridColors)
        val activeColorView = dialogView.findViewById<View>(R.id.viewSelectedColor)

        selectedColor = existingTag?.color ?: colors.random()
        etTagName.setText(existingTag?.name.orEmpty())
        activeColorView.backgroundTintList = ColorStateList.valueOf(selectedColor)

        val density = resources.displayMetrics.density
        val sizeInDp = (40 * density).toInt()
        val marginInDp = (6 * density).toInt()

        gridColors.removeAllViews()
        colors.forEach { color ->
            val colorView = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = sizeInDp
                    height = sizeInDp
                    setMargins(marginInDp, marginInDp, marginInDp, marginInDp)
                }
                background = resources.getDrawable(R.drawable.bg_circle_color, theme)
                backgroundTintList = ColorStateList.valueOf(color)
                setOnClickListener {
                    selectedColor = color
                    activeColorView.backgroundTintList = ColorStateList.valueOf(color)
                }
            }
            gridColors.addView(colorView)
        }

        val isEdit = existingTag != null
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (isEdit) "Sửa tag" else "Thêm tag")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Lưu" else "Thêm", null)
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = etTagName.text.toString().trim().replace(Regex("\\s+"), " ")
                if (name.length < 2) {
                    Toast.makeText(this, "Tên tag tối thiểu 2 ký tự", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (isEdit) {
                    updateTag(existingTag, name, selectedColor, dialog)
                } else {
                    saveTag(name, selectedColor, dialog)
                }
            }
        }

        dialog.show()
    }

    private fun saveTag(name: String, color: Int, dialog: androidx.appcompat.app.AlertDialog) {
        lifecycleScope.launch(Dispatchers.IO) {
            val existed = tagDao.getTagByName(name)
            if (existed != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageTagsActivity, "Tag đã tồn tại", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            tagDao.insertTag(TagEntity(name = name, color = color))
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ManageTagsActivity, "Đã thêm tag: $name", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }

    private fun updateTag(
        existingTag: TagEntity?,
        name: String,
        color: Int,
        dialog: androidx.appcompat.app.AlertDialog
    ) {
        if (existingTag == null) return

        lifecycleScope.launch(Dispatchers.IO) {
            val existed = tagDao.getTagByNameExceptId(name, existingTag.id)
            if (existed != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ManageTagsActivity, "Tag đã tồn tại", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            tagDao.updateTag(existingTag.copy(name = name, color = color))
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ManageTagsActivity, "Đã cập nhật tag", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }

    private fun showDeleteConfirmDialog(tag: TagEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa tag")
            .setMessage("Bạn có chắc muốn xóa tag '${tag.name}'? Các giao dịch sẽ giữ nguyên, chỉ bỏ nhãn.")
            .setPositiveButton("Xóa") { _, _ ->
                deleteTag(tag)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteTag(tag: TagEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            tagDao.deleteTag(tag)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ManageTagsActivity, "Đã xóa tag", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
