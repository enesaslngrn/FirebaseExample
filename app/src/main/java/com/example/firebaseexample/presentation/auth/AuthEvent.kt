package com.example.firebaseexample.presentation.auth

sealed class AuthEvent {
    data class SignIn(val email: String, val password: String) : AuthEvent()
    data class SignUp(val email: String, val password: String) : AuthEvent()
    data object SignOut : AuthEvent()
    data class SendPasswordReset(val email: String) : AuthEvent()
    data class SignInWithGoogle(val idToken: String) : AuthEvent()
    data object SendEmailVerification : AuthEvent()
    data object ClearError : AuthEvent()
    data object ClearSuccessMessage : AuthEvent()
    data class DeleteAccount(val currentPassword: String) : AuthEvent()
    data class ChangePassword(val currentPassword: String, val newPassword: String) : AuthEvent()
} 