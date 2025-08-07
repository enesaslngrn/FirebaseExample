package com.example.firebaseexample.domain.models

import com.google.firebase.auth.PhoneAuthProvider

sealed class AuthResult {
    data class Success(val user: User?) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
    data class CodeSent(val verificationId: String, val resendToken: PhoneAuthProvider.ForceResendingToken) : AuthResult()
} 