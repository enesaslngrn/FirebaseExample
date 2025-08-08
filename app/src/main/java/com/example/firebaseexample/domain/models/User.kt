package com.example.firebaseexample.domain.models

data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean? = null,
    val providers: List<String> = emptyList()
) {
    fun isGoogleUser(): Boolean = providers.contains("google.com")
    fun isPhoneUser(): Boolean = providers.contains("phone")
    fun isEmailPasswordUser(): Boolean = providers.contains("password")
} 