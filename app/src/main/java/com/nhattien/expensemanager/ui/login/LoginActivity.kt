package com.nhattien.expensemanager.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- GLOBAL CRASH HANDLER (Cài đặt ngay khi App mở) ---
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runOnUiThread {
                android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.title_launch_error))
                    .setMessage(getString(R.string.msg_launch_error, throwable.message, android.util.Log.getStackTraceString(throwable)))
                    .setPositiveButton(getString(R.string.action_copy_log)) { _, _ ->
                        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Crash Log", android.util.Log.getStackTraceString(throwable))
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, getString(R.string.msg_copied_short), Toast.LENGTH_SHORT).show()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        try {
            setContentView(R.layout.activity_login)

            val btnLogin = findViewById<Button>(R.id.btnLogin)
            val txtRegister = findViewById<TextView>(R.id.txtRegister)
            val edtUsername = findViewById<EditText>(R.id.edtUsername)
            val edtPassword = findViewById<EditText>(R.id.edtPassword)

            btnLogin.setOnClickListener {
                if (edtUsername.text.toString() == "admin" && edtPassword.text.toString() == "admin") {
                     // Fake login for admin
                }
                // Always go to main for now to unblock user
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            txtRegister.setOnClickListener {
                 Toast.makeText(this, getString(R.string.msg_register_disabled), Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
             e.printStackTrace()
             android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_error_init))
                .setMessage(e.toString())
                .show()
        }
    }
}
