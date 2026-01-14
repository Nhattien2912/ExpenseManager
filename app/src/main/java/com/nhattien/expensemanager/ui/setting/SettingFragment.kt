package com.nhattien.expensemanager.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.nhattien.expensemanager.R
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nhattien.expensemanager.utils.FirebaseUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingFragment : Fragment() {

    private val REQUEST_CODE_SIGN_IN = 100
    private var isBackupAction = true
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()

        // UI Components
        val swDarkMode = view.findViewById<SwitchMaterial>(R.id.swDarkMode)
        val btnLanguage = view.findViewById<View>(R.id.btnLanguage)
        val btnCurrency = view.findViewById<View>(R.id.btnCurrency)
        val txtLanguage = view.findViewById<android.widget.TextView>(R.id.txtLanguage)
        val txtCurrency = view.findViewById<android.widget.TextView>(R.id.txtCurrency)
        

        val btnLogout = view.findViewById<View>(R.id.btnLogout)
        val dividerLogout = view.findViewById<View>(R.id.dividerLogout)
        val btnDeleteAll = view.findViewById<View>(R.id.btnDeleteAll)
        
        val tvSyncTitle = view.findViewById<android.widget.TextView>(R.id.tvSyncTitle)
        val tvSyncSubtitle = view.findViewById<android.widget.TextView>(R.id.tvSyncSubtitle)

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
             Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
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

        // ======================================
        // 4. CLOUD ACCOUNT & SYNC LOGIC
        // ======================================
        
        val btnAccountAction = view.findViewById<View>(R.id.btnAccountAction)
        val layoutSyncActions = view.findViewById<View>(R.id.layoutSyncActions)
        val btnUpload = view.findViewById<View>(R.id.btnUpload)
        val btnDownload = view.findViewById<View>(R.id.btnDownload)

        // Function to update UI based on Auth state
        fun updateSyncUI(user: com.google.firebase.auth.FirebaseUser?) {
            if (user != null) {
                // Logged In
                tvSyncTitle.text = "Đã kết nối: ${user.email}"
                tvSyncSubtitle.text = "Tài khoản bảo mật"
                
                // Show actions
                layoutSyncActions.visibility = View.VISIBLE
                btnLogout.visibility = View.VISIBLE
                dividerLogout.visibility = View.VISIBLE
                
                // Info only, cant log in again
                btnAccountAction.setOnClickListener {
                     Toast.makeText(context, "Đã đăng nhập: ${user.email}", Toast.LENGTH_SHORT).show()
                }

                // Actions
                btnUpload.setOnClickListener {
                    performBackup(user.uid)
                }

                btnDownload.setOnClickListener {
                     android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Tải dữ liệu về?")
                        .setMessage("Hành động này sẽ thay thế dữ liệu hiện tại bằng dữ liệu trên Cloud. Bạn có chắc không?")
                        .setPositiveButton("Tải về") { _, _ ->
                            performRestore(user.uid)
                        }
                        .setNegativeButton("Hủy", null)
                        .show()
                }

            } else {
                // Not Logged In
                tvSyncTitle.text = "Kết nối Google Cloud"
                tvSyncSubtitle.text = "Đăng nhập để đồng bộ dữ liệu"
                
                // Hide actions
                layoutSyncActions.visibility = View.GONE
                btnLogout.visibility = View.GONE
                dividerLogout.visibility = View.GONE
                
                btnAccountAction.setOnClickListener {
                    signIn()
                }
            }
        }

        // Initial check
        updateSyncUI(auth.currentUser)

        // Logout
        btnLogout.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất?")
                .setMessage("Bạn có muốn đăng xuất khỏi Cloud không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    auth.signOut()
                    GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                    Toast.makeText(context, "Đã đăng xuất!", Toast.LENGTH_SHORT).show()
                    updateSyncUI(null)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // --- DELETE ALL ---
        btnDeleteAll.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("CẢNH BÁO NGUY HIỂM ⚠️")
                .setMessage("Bạn có chắc chắn muốn XÓA SẠCH toàn bộ dữ liệu không? Hành động này không thể hoàn tác!")
                .setPositiveButton("XÓA HẾT") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            com.nhattien.expensemanager.data.database.AppDatabase.getInstance(requireContext())
                                .transactionDao().deleteAll()
                             com.nhattien.expensemanager.data.database.AppDatabase.getInstance(requireContext())
                                .debtDao().deleteAll() 
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Đã xóa sạch dữ liệu!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                             withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun signIn() {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        startActivityForResult(googleSignInClient.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-In thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(context, "Đăng nhập thành công! Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
                    if (user != null) performRestore(user.uid)
                } else {
                    Toast.makeText(context, "Lỗi xác thực: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun performBackup(uid: String) {
        Toast.makeText(context, "Đang đồng bộ lên Cloud... ☁️", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = FirebaseUtils.backupData(requireContext(), uid)
            if (result.first) {
                Toast.makeText(context, "Đã lưu dữ liệu thành công! ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Lỗi: ${result.second}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performRestore(uid: String) {
        Toast.makeText(context, "Đang tải dữ liệu về... 🔄", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = FirebaseUtils.restoreData(requireContext(), uid)
            if (result.first) {
                Toast.makeText(context, "Đã đồng bộ xong! ✅", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Lỗi: ${result.second}", Toast.LENGTH_LONG).show()
            }
        }
    }
}