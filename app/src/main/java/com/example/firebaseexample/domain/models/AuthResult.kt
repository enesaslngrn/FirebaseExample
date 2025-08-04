package com.example.firebaseexample.domain.models

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
    data object AccountDeleted : AuthResult()
} 