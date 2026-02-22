package com.nhattien.expensemanager.ui.lock

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.main.MainActivity
import com.nhattien.expensemanager.utils.BiometricHelper
import com.google.android.material.button.MaterialButton

class BiometricLockActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_lock)

        setupBiometric()

        // Auto-show prompt on launch
        showBiometricPrompt()

        // Retry button
        findViewById<MaterialButton>(R.id.btnUnlock).setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun setupBiometric() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    navigateToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        // User cancelled - stay on lock screen
                        Toast.makeText(this@BiometricLockActivity,
                            "Bấm nút để thử lại", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@BiometricLockActivity,
                            "Lỗi: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@BiometricLockActivity,
                        "Không nhận diện được. Thử lại!", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Mở khóa Expense Manager")
            .setSubtitle("Sử dụng vân tay hoặc khuôn mặt để xác thực")
            .setNegativeButtonText("Hủy")
            .build()
    }

    private fun showBiometricPrompt() {
        if (BiometricHelper.canAuthenticate(this)) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            // Device doesn't support biometric, skip lock
            Toast.makeText(this, "Thiết bị không hỗ trợ sinh trắc học", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("BIOMETRIC_PASSED", true)
        })
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back press from bypassing the lock screen
        // Do nothing
    }
}
