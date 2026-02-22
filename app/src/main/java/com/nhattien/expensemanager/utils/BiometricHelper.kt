package com.nhattien.expensemanager.utils

import android.content.Context
import androidx.biometric.BiometricManager

object BiometricHelper {

    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getBiometricStatus(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Sẵn sàng"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Thiết bị không hỗ trợ"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Tạm thời không khả dụng"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Chưa đăng ký vân tay"
            else -> "Không xác định"
        }
    }

    fun isBiometricEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("biometric_enabled", false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }
}
