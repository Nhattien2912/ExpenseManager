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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.activityViewModels
import com.nhattien.expensemanager.viewmodel.MainViewModel
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
        val prefs = requireContext().getSharedPreferences("expense_manager", android.content.Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("KEY_DARK_MODE", false)
        swDarkMode.isChecked = isDarkMode

        swDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("KEY_DARK_MODE", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Toast.makeText(context, getString(R.string.msg_dark_mode_on), Toast.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Toast.makeText(context, getString(R.string.msg_dark_mode_off), Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Language Selection
        btnLanguage.setOnClickListener {
             Toast.makeText(context, getString(R.string.msg_feature_dev), Toast.LENGTH_SHORT).show()
        }

        // 3. Currency Selection
        btnCurrency.setOnClickListener {
            val currencies = arrayOf("VND (đ)", "USD ($)")
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.title_select_currency))
                .setItems(currencies) { _, which ->
                    txtCurrency.text = if (which == 0) "VND" else "USD"
                    com.nhattien.expensemanager.utils.CurrencyUtils.checkCurrency = which
                    Toast.makeText(context, "${getString(R.string.msg_currency_changed)} ${if (which == 0) "VND" else "USD"}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // ======================================
        // 4. CLOUD ACCOUNT & SYNC LOGIC
        // ======================================
        
        view.findViewById<View>(R.id.btnManageCategories).setOnClickListener {
             startActivity(Intent(requireContext(), com.nhattien.expensemanager.ui.category.CategoryManagerActivity::class.java))
        }

        val btnAccountAction = view.findViewById<View>(R.id.btnAccountAction)
        val layoutSyncActions = view.findViewById<View>(R.id.layoutSyncActions)
        val btnUpload = view.findViewById<View>(R.id.btnUpload)
        val btnDownload = view.findViewById<View>(R.id.btnDownload)

        // Function to update UI based on Auth state
        fun updateSyncUI(user: com.google.firebase.auth.FirebaseUser?) {
            if (user != null) {
                // Logged In
                tvSyncTitle.text = "${getString(R.string.msg_connected)} ${user.email}"
                tvSyncSubtitle.text = getString(R.string.msg_secure_account)
                
                // Show actions
                layoutSyncActions.visibility = View.VISIBLE
                btnLogout.visibility = View.VISIBLE
                dividerLogout.visibility = View.VISIBLE
                
                // Info only, cant log in again
                btnAccountAction.setOnClickListener {
                     Toast.makeText(context, "${getString(R.string.msg_connected)} ${user.email}", Toast.LENGTH_SHORT).show()
                }

                // Actions
                btnUpload.setOnClickListener {
                    performBackup(user.uid)
                }

                btnDownload.setOnClickListener {
                     android.app.AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.title_confirm_restore))
                        .setMessage(getString(R.string.msg_confirm_restore))
                        .setPositiveButton(getString(R.string.label_restore)) { _, _ ->
                            performRestore(user.uid)
                        }
                        .setNegativeButton("Hủy", null)
                        .show()
                }

            } else {
                // Not Logged In
                tvSyncTitle.text = getString(R.string.label_connect_account)
                tvSyncSubtitle.text = getString(R.string.label_sync_subtitle)
                
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
                .setTitle(getString(R.string.title_confirm_logout))
                .setMessage(getString(R.string.msg_confirm_logout))
                .setPositiveButton(getString(R.string.action_logout)) { _, _ ->
                    auth.signOut()
                    GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
                    Toast.makeText(context, getString(R.string.action_logout), Toast.LENGTH_SHORT).show()
                    updateSyncUI(null)
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        // --- DELETE ALL ---
        btnDeleteAll.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.title_danger_zone))
                .setMessage(getString(R.string.msg_confirm_delete_all))
                .setPositiveButton(getString(R.string.action_delete_all)) { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            com.nhattien.expensemanager.data.database.AppDatabase.getInstance(requireContext())
                                .transactionDao().deleteAll()
                             com.nhattien.expensemanager.data.database.AppDatabase.getInstance(requireContext())
                                .debtDao().deleteAll() 
                            
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, getString(R.string.msg_data_deleted), Toast.LENGTH_SHORT).show()
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
        
        // --- TUTORIAL BUTTON ---
        view.findViewById<View>(R.id.btnShowTutorial).setOnClickListener {
            // Quay về MainActivity và hiển thị tutorial
            (activity as? com.nhattien.expensemanager.ui.main.MainActivity)?.let { mainActivity ->
                mainActivity.loadFragment(com.nhattien.expensemanager.ui.main.MainFragment())
                mainActivity.window.decorView.post {
                    mainActivity.showTutorial()
                }
            }
        }

        // --- EXPORT CSV ---
        // Use activityViewModels to share data with MainFragment
        val mainViewModel: com.nhattien.expensemanager.viewmodel.MainViewModel by activityViewModels()
        
        view.findViewById<View>(R.id.btnExportCsv)?.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val transactions = mainViewModel.allTransactions.value // Get current value
                if (transactions.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, getString(R.string.msg_no_data_export), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // Convert mapping if needed. CsvUtils takes TransactionWithCategory
                // MainViewModel.allTransactions is List<TransactionWithCategory>
                
                val uri = com.nhattien.expensemanager.utils.CsvUtils.exportTransactionsToCsv(requireContext(), transactions)
                
                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv" // or "application/vnd.ms-excel"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Backup Expense Manager")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.label_export)))
                    } else {
                         Toast.makeText(context, "Lỗi khi xuất file!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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