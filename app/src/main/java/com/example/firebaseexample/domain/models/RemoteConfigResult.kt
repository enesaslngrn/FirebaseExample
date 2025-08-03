package com.example.firebaseexample.domain.models

sealed class RemoteConfigResult {
    data class Success(val forceUpdateVersion: String, val maintenanceMode: Boolean) : RemoteConfigResult()
    data class Error(val message: String) : RemoteConfigResult()
    data object Loading : RemoteConfigResult()
} 