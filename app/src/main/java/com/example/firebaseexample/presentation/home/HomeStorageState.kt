package com.example.firebaseexample.presentation.home

data class HomeStorageState(
    val isLoading: Boolean = false,
    val profilePhotoUrl: String? = null,
    val error: String? = null,
    val successMessage: String? = null
) 