package com.example.fitnesstracker

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstracker.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvForgotPassword.setOnClickListener { sendPasswordReset() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        // Already signed in (and verified)? Skip the login screen.
        if (AuthConfig.isAuthorized(auth.currentUser)) goToMain()
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Enter your password"
            return
        }

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                setLoading(false)
                when {
                    user == null -> showMessage("Sign-in failed. Please try again.")
                    AuthConfig.REQUIRE_VERIFIED_EMAIL && !user.isEmailVerified ->
                        promptUnverified(user)
                    else -> goToMain()
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                binding.etPassword.text?.clear()
                showMessage(messageFor(e))
            }
    }

    private fun promptUnverified(user: FirebaseUser) {
        AlertDialog.Builder(this)
            .setTitle("Verify your email")
            .setMessage("Please verify your email address before signing in. Check your inbox for the verification link.")
            .setCancelable(false)
            .setPositiveButton("Resend email") { _, _ ->
                user.sendEmailVerification().addOnCompleteListener {
                    auth.signOut()
                    showMessage("Verification email sent. Check your inbox.")
                }
            }
            .setNegativeButton("OK") { _, _ -> auth.signOut() }
            .show()
    }

    private fun sendPasswordReset() {
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter your email above first"
            return
        }
        binding.tilEmail.error = null
        auth.sendPasswordResetEmail(email).addOnCompleteListener {
            // Same message whether or not the account exists (prevents account enumeration).
            showMessage("If an account exists for that email, a reset link has been sent.")
        }
    }

    private fun messageFor(e: Exception): String = when (e) {
        // Deliberately generic: don't reveal whether the email exists.
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
        is FirebaseNetworkException -> "Network error. Check your connection."
        else -> "Sign-in failed. Please try again."
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.tvRegister.isEnabled = !loading
        binding.tvForgotPassword.isEnabled = !loading
    }

    private fun showMessage(text: String) =
        Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show()

    private fun goToMain() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}