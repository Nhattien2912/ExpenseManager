package com.nhattien.expensemanager.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nhattien.expensemanager.R

class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swDarkMode = view.findViewById<SwitchMaterial>(R.id.swDarkMode)

        // 1. Kiểm tra chế độ hiện tại để bật/tắt nút switch cho đúng
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        swDarkMode.isChecked = (currentMode == AppCompatDelegate.MODE_NIGHT_YES)

        // 2. Xử lý khi gạt nút
        swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(context, "Đã bật Dark Mode", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(context, "Đã tắt Dark Mode", Toast.LENGTH_SHORT).show()
            }
        }
    }
}