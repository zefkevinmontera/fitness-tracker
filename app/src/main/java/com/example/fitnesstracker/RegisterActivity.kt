package com.example.fitnesstracker

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstracker.databinding.ActivityRegisterBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { attemptRegister() }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val name = binding.etName.text?.toString().orEmpty().trim()
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        if (name.isEmpty()) {
            binding.tilName.error = "Enter your name"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
            return
        }
        if (!AuthConfig.isStrongPassword(password)) {
            binding.tilPassword.error =
                "Use at least ${AuthConfig.MIN_PASSWORD_LENGTH} characters, including a letter and a number"
            return
        }
        if (password != confirm) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return
        }

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    setLoading(false)
                    showMessage("Registration failed. Please try again.")
                    return@addOnSuccessListener
                }
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profile).addOnCompleteListener {
                    user.sendEmailVerification().addOnCompleteListener {
                        // Force a fresh sign-in after verification.
                        auth.signOut()
                        setLoading(false)
                        Toast.makeText(
                            this,
                            "Account created. Check your email to verify, then sign in.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish() // back to LoginActivity
                    }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                showMessage(messageFor(e))
            }
    }

    private fun messageFor(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthWeakPasswordException -> "That password is too weak."
        is FirebaseAuthInvalidCredentialsException -> "That email address is not valid."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
        is FirebaseNetworkException -> "Network error. Check your connection."
        else -> "Registration failed. Please try again."
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
        binding.tvLogin.isEnabled = !loading
    }

    private fun showMessage(text: String) =
        Snackbar.make(binding.root, text, Snackbar.LENGTH_LONG).show()
}