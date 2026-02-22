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
    
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
             Toast.makeText(context, "Đã cấp quyền thông báo! Hãy bật lại tính năng.", Toast.LENGTH_SHORT).show()
        } else {
             Toast.makeText(context, "Cần quyền thông báo để hoạt động", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkNotificationPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

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

        // --- Biometric Lock ---
        val swBiometricLock = view.findViewById<SwitchMaterial>(R.id.swBiometricLock)
        val txtBiometricStatus = view.findViewById<android.widget.TextView>(R.id.txtBiometricStatus)
        
        val canUseBiometric = com.nhattien.expensemanager.utils.BiometricHelper.canAuthenticate(requireContext())
        swBiometricLock.isEnabled = canUseBiometric
        swBiometricLock.isChecked = com.nhattien.expensemanager.utils.BiometricHelper.isBiometricEnabled(requireContext())
        txtBiometricStatus.text = if (canUseBiometric) {
            if (swBiometricLock.isChecked) "Đang bật bảo vệ" else "Yêu cầu xác thực khi mở app"
        } else {
            com.nhattien.expensemanager.utils.BiometricHelper.getBiometricStatus(requireContext())
        }
        
        swBiometricLock.setOnCheckedChangeListener { _, isChecked ->
            com.nhattien.expensemanager.utils.BiometricHelper.setBiometricEnabled(requireContext(), isChecked)
            txtBiometricStatus.text = if (isChecked) "Đang bật bảo vệ" else "Yêu cầu xác thực khi mở app"
            val msg = if (isChecked) "Đã bật khóa vân tay 🔒" else "Đã tắt khóa vân tay 🔓"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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

        // 4. Manage Recurring
        val btnManageRecurring = view.findViewById<View>(R.id.btnManageRecurring)
        btnManageRecurring.setOnClickListener {
            startActivity(Intent(context, com.nhattien.expensemanager.ui.recurring.ManageRecurringActivity::class.java))
        }

    // ... ui logic
        // --- 5. Notifications Logic ---
        val swDailyReminder = view.findViewById<SwitchMaterial>(R.id.swDailyReminder)
        val layoutReminderTime = view.findViewById<View>(R.id.layoutReminderTime)
        val txtReminderTime = view.findViewById<android.widget.TextView>(R.id.txtReminderTime)
        val swBudgetAlert = view.findViewById<SwitchMaterial>(R.id.swBudgetAlert)
        val swDebtReminder = view.findViewById<SwitchMaterial>(R.id.swDebtReminder)
        
        // Reminder
        val isReminderEnabled = prefs.getBoolean("KEY_DAILY_REMINDER_ENABLED", false)
        val reminderHour = prefs.getInt("KEY_REMINDER_HOUR", 20)
        val reminderMinute = prefs.getInt("KEY_REMINDER_MINUTE", 0)
        
        swDailyReminder.isChecked = isReminderEnabled
        layoutReminderTime.visibility = if (isReminderEnabled) View.VISIBLE else View.GONE
        txtReminderTime.text = String.format("%02d:%02d", reminderHour, reminderMinute)
        
        swDailyReminder.setOnCheckedChangeListener { buttonView, isChecked ->
             if (isChecked) {
                 if (!checkNotificationPermission()) {
                     buttonView.isChecked = false
                     requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                     return@setOnCheckedChangeListener
                 }
                 
                 prefs.edit().putBoolean("KEY_DAILY_REMINDER_ENABLED", true).apply()
                 layoutReminderTime.visibility = View.VISIBLE
                 com.nhattien.expensemanager.utils.NotificationHelper.scheduleDailyReminder(requireContext(), reminderHour, reminderMinute)
                 Toast.makeText(context, "Đã bật nhắc nhở", Toast.LENGTH_SHORT).show()
             } else {
                 prefs.edit().putBoolean("KEY_DAILY_REMINDER_ENABLED", false).apply()
                 layoutReminderTime.visibility = View.GONE
                 com.nhattien.expensemanager.utils.NotificationHelper.cancelDailyReminder(requireContext())
             }
        }
        
         layoutReminderTime.setOnClickListener {
            val hour = prefs.getInt("KEY_REMINDER_HOUR", 20)
            val minute = prefs.getInt("KEY_REMINDER_MINUTE", 0)
            
            android.app.TimePickerDialog(requireContext(), { _, h, m ->
                prefs.edit().putInt("KEY_REMINDER_HOUR", h).putInt("KEY_REMINDER_MINUTE", m).apply()
                txtReminderTime.text = String.format("%02d:%02d", h, m)
                // Reschedule if enabled
                if (swDailyReminder.isChecked) {
                     com.nhattien.expensemanager.utils.NotificationHelper.scheduleDailyReminder(requireContext(), h, m)
                     Toast.makeText(context, "Đã đặt lại giờ nhắc!", Toast.LENGTH_SHORT).show()
                }
            }, hour, minute, true).show()
        }
        
        // Debug: Long click to test notification immediately
        layoutReminderTime.setOnLongClickListener {
             try {
                 Toast.makeText(context, "Test Notification ngay lập tức...", Toast.LENGTH_SHORT).show()
                 com.nhattien.expensemanager.utils.NotificationHelper.showReminderNotification(requireContext())
             } catch (e: Exception) {
                 Toast.makeText(context, "Lỗi Test: ${e.message}", Toast.LENGTH_LONG).show()
             }
             true
        }
        
        // Budget Alert
        swBudgetAlert.isChecked = prefs.getBoolean("KEY_BUDGET_ALERT", true)
        swBudgetAlert.setOnCheckedChangeListener { buttonView, isChecked ->
             if (isChecked) {
                 if (!checkNotificationPermission()) {
                     buttonView.isChecked = false
                     requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                     return@setOnCheckedChangeListener
                 }
                 
                 prefs.edit().putBoolean("KEY_BUDGET_ALERT", true).apply()
                 
                 // Run immediately first
                 val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<com.nhattien.expensemanager.worker.BudgetCheckWorker>().build()
                 androidx.work.WorkManager.getInstance(requireContext()).enqueue(oneTimeRequest)
                 
                 // Then schedule periodic
                 val request = androidx.work.PeriodicWorkRequestBuilder<com.nhattien.expensemanager.worker.BudgetCheckWorker>(
                    12, java.util.concurrent.TimeUnit.HOURS
                  ).build()
                   androidx.work.WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                    "budget_check", androidx.work.ExistingPeriodicWorkPolicy.KEEP, request
                  )
                 Toast.makeText(context, "Đã bật cảnh báo chi tiêu", Toast.LENGTH_SHORT).show()
             } else {
                 prefs.edit().putBoolean("KEY_BUDGET_ALERT", false).apply()
                 androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("budget_check")
             }
        }
        
        // Debt Reminder
         swDebtReminder.isChecked = prefs.getBoolean("KEY_DEBT_REMINDER", true)
         swDebtReminder.setOnCheckedChangeListener { buttonView, isChecked ->
             if (isChecked) {
                 if (!checkNotificationPermission()) {
                     buttonView.isChecked = false
                     requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                     return@setOnCheckedChangeListener
                 }
                 
                 prefs.edit().putBoolean("KEY_DEBT_REMINDER", true).apply()
                 
                 // Run immediately first
                 val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<com.nhattien.expensemanager.worker.DebtReminderWorker>().build()
                 androidx.work.WorkManager.getInstance(requireContext()).enqueue(oneTimeRequest)
                 
                 // Then schedule periodic
                 val request = androidx.work.PeriodicWorkRequestBuilder<com.nhattien.expensemanager.worker.DebtReminderWorker>(
                    1, java.util.concurrent.TimeUnit.DAYS
                  ).build()
                   androidx.work.WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                    "debt_reminder", androidx.work.ExistingPeriodicWorkPolicy.KEEP, request
                  )
                 Toast.makeText(context, "Đã bật nhắc nợ", Toast.LENGTH_SHORT).show()
             } else {
                 prefs.edit().putBoolean("KEY_DEBT_REMINDER", false).apply()
                 androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("debt_reminder")
             }
         }
         
         // Init workers if needed initially (only if turned on)
         if (prefs.getBoolean("KEY_DEBT_REMINDER", true)) {
              // Run immediately first
              val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<com.nhattien.expensemanager.worker.DebtReminderWorker>().build()
              androidx.work.WorkManager.getInstance(requireContext()).enqueue(oneTimeRequest)
              
              // Then schedule periodic
              val request = androidx.work.PeriodicWorkRequestBuilder<com.nhattien.expensemanager.worker.DebtReminderWorker>(
                    1, java.util.concurrent.TimeUnit.DAYS
                  ).build()
              androidx.work.WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                    "debt_reminder", androidx.work.ExistingPeriodicWorkPolicy.KEEP, request
              )
         }
         
         if (prefs.getBoolean("KEY_BUDGET_ALERT", true)) {
              // Run immediately first
              val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<com.nhattien.expensemanager.worker.BudgetCheckWorker>().build()
              androidx.work.WorkManager.getInstance(requireContext()).enqueue(oneTimeRequest)
              
              // Then schedule periodic
              val request = androidx.work.PeriodicWorkRequestBuilder<com.nhattien.expensemanager.worker.BudgetCheckWorker>(
                    12, java.util.concurrent.TimeUnit.HOURS
                  ).build()
              androidx.work.WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                    "budget_check", androidx.work.ExistingPeriodicWorkPolicy.KEEP, request
              )
         }

        // ======================================
        // 4. CLOUD ACCOUNT & SYNC LOGIC
        // ======================================
        
        view.findViewById<View>(R.id.btnManageCategories).setOnClickListener {
             startActivity(Intent(requireContext(), com.nhattien.expensemanager.ui.category.CategoryManagerActivity::class.java))
        }

        view.findViewById<View>(R.id.btnManageTags).setOnClickListener {
             startActivity(Intent(requireContext(), com.nhattien.expensemanager.ui.tag.ManageTagsActivity::class.java))
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