---
name: Biometric Authentication
description: Xác thực bằng vân tay, Face ID để bảo vệ ứng dụng.
---

# Biometric Authentication

## Dependencies

```groovy
dependencies {
    implementation("androidx.biometric:biometric:1.1.0")
}
```

## Kiểm tra khả năng sử dụng

```kotlin
object BiometricHelper {
    
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    fun getBiometricStatus(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Sẵn sàng"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Thiết bị không hỗ trợ"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Tạm thời không khả dụng"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Chưa đăng ký vân tay"
            else -> "Không xác định"
        }
    }
}
```

## Hiển thị Prompt xác thực

```kotlin
class LockActivity : AppCompatActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupBiometric()
        showBiometricPrompt()
    }

    private fun setupBiometric() {
        val executor = ContextCompat.getMainExecutor(this)
        
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Mở khóa thành công
                    navigateToMain()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Lỗi xác thực
                    Toast.makeText(this@LockActivity, errString, Toast.LENGTH_SHORT).show()
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Vân tay không khớp
                    Toast.makeText(this@LockActivity, "Không nhận diện được", Toast.LENGTH_SHORT).show()
                }
            })
        
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Mở khóa Expense Manager")
            .setSubtitle("Sử dụng vân tay để xác thực")
            .setNegativeButtonText("Sử dụng mật khẩu")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    private fun showBiometricPrompt() {
        if (BiometricHelper.canAuthenticate(this)) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            // Fallback to PIN/password
            showPasswordDialog()
        }
    }
}
```

## Bật/tắt trong Settings

```kotlin
// Lưu preference
prefs.edit().putBoolean("biometric_enabled", true).apply()

// Kiểm tra khi app start
if (prefs.getBoolean("biometric_enabled", false)) {
    startActivity(Intent(this, LockActivity::class.java))
}
```
