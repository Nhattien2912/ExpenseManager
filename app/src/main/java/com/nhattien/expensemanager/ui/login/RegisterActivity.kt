package com.nhattien.expensemanager.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nhattien.expensemanager.ui.main.MainActivity
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.utils.FirebaseUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegisterFinal)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                        // Initial sync (Backup mostly empty or default data)
                        syncAndGo(auth.currentUser!!.uid)
                    } else {
                        Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvBackToLogin.setOnClickListener {
            finish() // Return to Login
        }
    }

    private fun syncAndGo(uid: String) {
        Toast.makeText(this, "Đang thiết lập dữ liệu...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            // New account: typically push whatever local state exists (might be empty)
            val result = FirebaseUtils.backupData(this@RegisterActivity, uid)
             if (!result.first) {
                 Toast.makeText(this@RegisterActivity, "Lỗi thiết lập: ${result.second}", Toast.LENGTH_LONG).show()
            }
            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
            finishAffinity() // Close all login/register screens
        }
    }
}
