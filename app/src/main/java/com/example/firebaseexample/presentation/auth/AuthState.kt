package com.example.firebaseexample.presentation.auth

import com.example.firebaseexample.domain.models.User

data class AuthState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isInitialized: Boolean = false,
    val verificationMessage: String? = null,
    val accountDeleted: Boolean = false
)