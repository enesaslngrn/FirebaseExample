package com.example.firebaseexample.presentation.auth

sealed class AuthEvent {
    data class SignIn(val email: String, val password: String) : AuthEvent()
    data class SignUp(val email: String, val password: String) : AuthEvent()
    object SignOut : AuthEvent()
    data class SendPasswordReset(val email: String) : AuthEvent()
    object ClearError : AuthEvent()
} 