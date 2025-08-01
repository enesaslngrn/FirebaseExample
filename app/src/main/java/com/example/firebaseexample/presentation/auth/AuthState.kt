package com.example.firebaseexample.presentation.auth

import com.example.firebaseexample.domain.models.User

data class AuthState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val isEmailVerified: Boolean? = null,
    val verificationMessage: String? = null,
    val isInitialized: Boolean = false
) 