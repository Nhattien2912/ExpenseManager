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
        val btnLanguage = view.findViewById<View>(R.id.btnLanguage)
        val btnCurrency = view.findViewById<View>(R.id.btnCurrency)
        val txtLanguage = view.findViewById<android.widget.TextView>(R.id.txtLanguage)
        val txtCurrency = view.findViewById<android.widget.TextView>(R.id.txtCurrency)

        // 1. Setup Dark Mode
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        swDarkMode.isChecked = (currentMode == AppCompatDelegate.MODE_NIGHT_YES)

        swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(context, "Đã bật Dark Mode", Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(context, "Đã tắt Dark Mode", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Language Selection
        btnLanguage.setOnClickListener {
            val languages = arrayOf("Tiếng Việt", "English")
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Chọn ngôn ngữ")
                .setItems(languages) { _, which ->
                    txtLanguage.text = languages[which]
                    Toast.makeText(context, "Đã chọn: ${languages[which]} (Tính năng đang phát triển)", Toast.LENGTH_SHORT).show()
                    // TODO: Implement Locale switching logic later
                }
                .show()
        }

        // 3. Currency Selection
        btnCurrency.setOnClickListener {
            val currencies = arrayOf("VND (đ)", "USD ($)")
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Chọn tiền tệ")
                .setItems(currencies) { _, which ->
                    txtCurrency.text = if (which == 0) "VND" else "USD"
                    com.nhattien.expensemanager.utils.CurrencyUtils.checkCurrency = which
                    Toast.makeText(context, "Đã đổi tiền tệ sang: ${if (which == 0) "VND" else "USD"}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }
}