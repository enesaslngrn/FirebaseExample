package com.example.firebaseexample.presentation.auth

import com.example.firebaseexample.domain.models.User
import com.google.firebase.auth.PhoneAuthProvider

data class AuthState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isInitialized: Boolean = false,
    val verificationMessage: String? = null,
    val accountDeleted: Boolean = false,
    // Phone Auth states
    val isPhoneAuthMode: Boolean = false,
    val phoneNumber: String = "",
    val verificationId: String = "",
    val resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    val isCodeSent: Boolean = false,
    val isVerifyingCode: Boolean = false,
    // Timer states
    val remainingTime: Int = 0,
    val isTimerActive: Boolean = false,
    val isCodeExpired: Boolean = false
)