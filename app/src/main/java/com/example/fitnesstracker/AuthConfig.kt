package com.example.fitnesstracker

import com.google.firebase.auth.FirebaseUser

object AuthConfig {
    /** Block access until the user clicks the link in the verification email. */
    const val REQUIRE_VERIFIED_EMAIL = false
    const val MIN_PASSWORD_LENGTH = 8

    /** At least MIN_PASSWORD_LENGTH chars, one letter and one digit. */
    fun isStrongPassword(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH &&
                password.any { it.isLetter() } &&
                password.any { it.isDigit() }

    /** Single source of truth for "may this user see protected screens?" */
    fun isAuthorized(user: FirebaseUser?): Boolean =
        user != null && (!REQUIRE_VERIFIED_EMAIL || user.isEmailVerified)
}