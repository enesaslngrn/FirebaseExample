package com.example.firebaseexample.presentation.splash

data class SplashState(
    val isLoading: Boolean = true,
    val isForceUpdateRequired: Boolean = false,
    val isMaintenanceMode: Boolean = false,
    val error: String? = null,
    val canProceed: Boolean = false
) 