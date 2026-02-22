package com.nhattien.expensemanager.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.nhattien.expensemanager.R
import com.nhattien.expensemanager.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    // Google Sign In Result Launcher
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("LoginActivity", "Google sign in failed", e)
                Toast.makeText(this, "Google Sign In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()
        
        // Check if user is already signed in
        if (firebaseAuth.currentUser != null) {
            navigateToMain()
            return
        }

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

            // Configure Google Sign In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Requires google-services.json
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            val btnLogin = findViewById<Button>(R.id.btnLogin)
            val btnGoogleLogin = findViewById<SignInButton>(R.id.btnGoogleLogin)
            val txtRegister = findViewById<TextView>(R.id.txtRegister)
            val edtUsername = findViewById<EditText>(R.id.edtUsername)
            val edtPassword = findViewById<EditText>(R.id.edtPassword)
            
            // Customize Google Button
            btnGoogleLogin.setSize(SignInButton.SIZE_WIDE)

            btnLogin.setOnClickListener {
                if (edtUsername.text.toString() == "admin" && edtPassword.text.toString() == "admin") {
                     // Fake login for admin - Still allow for dev/testing
                     navigateToMain()
                } else {
                    Toast.makeText(this, "Invalid credentials (use admin/admin or Google)", Toast.LENGTH_SHORT).show()
                }
            }
            
            btnGoogleLogin.setOnClickListener {
                signIn()
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
    
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d("LoginActivity", "firebaseAuthWithGoogle:success")
                    navigateToMain()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("LoginActivity", "firebaseAuthWithGoogle:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication Failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
