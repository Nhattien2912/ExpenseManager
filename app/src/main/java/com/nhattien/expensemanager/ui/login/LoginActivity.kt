package com.nhattien.expensemanager.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nhattien.expensemanager.ui.main.MainActivity
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.utils.FirebaseUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val REQUEST_CODE_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Check if already logged in
        if (auth.currentUser != null) {
            goToMain()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val btnGoogleSignIn = findViewById<MaterialCardView>(R.id.btnGoogleSignIn)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Restore data on login
                            syncAndGo(auth.currentUser!!.uid)
                        } else {
                            Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Vui lòng nhập Email & Mật khẩu", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnGoogleSignIn.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(this, gso)
            startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Use account.id as fallback if token is missing due to config
                if (account.idToken != null) {
                    firebaseAuthWithGoogle(account.idToken!!)
                } else {
                     // Fallback for missing web_client_id
                    syncAndGo(account.id!!, isRestore = true) 
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result.additionalUserInfo?.isNewUser ?: false
                    // If login existing -> Restore. If new -> Backup (save local data to cloud account)
                    syncAndGo(auth.currentUser!!.uid, isRestore = !isNewUser)
                } else {
                    Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun syncAndGo(uid: String, isRestore: Boolean = true) {
        Toast.makeText(this, "Đang đồng bộ dữ liệu...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            if (isRestore) {
                val result = FirebaseUtils.restoreData(this@LoginActivity, uid)
                if (!result.first) {
                     Toast.makeText(this@LoginActivity, "Lỗi đồng bộ: ${result.second}", Toast.LENGTH_LONG).show()
                }
            } else {
                val result = FirebaseUtils.backupData(this@LoginActivity, uid)
                if (!result.first) {
                     Toast.makeText(this@LoginActivity, "Lỗi sao lưu: ${result.second}", Toast.LENGTH_LONG).show()
                }
            }
            goToMain()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
