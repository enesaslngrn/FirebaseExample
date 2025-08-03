package com.example.firebaseexample.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseexample.BuildConfig
import com.example.firebaseexample.domain.models.RemoteConfigResult
import com.example.firebaseexample.domain.usecases.FetchRemoteConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val fetchRemoteConfigUseCase: FetchRemoteConfigUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        fetchRemoteConfig()
    }

    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            fetchRemoteConfigUseCase().collect { result ->
                when (result) {
                    is RemoteConfigResult.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    is RemoteConfigResult.Success -> {
                        handleRemoteConfigSuccess(result.forceUpdateVersion, result.maintenanceMode)
                    }
                    is RemoteConfigResult.Error -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = result.message,
                                canProceed = true // Allow proceed on error
                            )
                        }
                        Timber.e("Remote Config error: ${result.message}")
                    }
                }
            }
        }
    }

    private fun handleRemoteConfigSuccess(forceUpdateVersion: String, maintenanceMode: Boolean) {
        val currentVersion = BuildConfig.VERSION_NAME
        val isForceUpdateRequired = isVersionLower(currentVersion, forceUpdateVersion)
        
        Timber.d("Current version: $currentVersion, Force update version: $forceUpdateVersion")
        Timber.d("Is force update required: $isForceUpdateRequired, Maintenance mode: $maintenanceMode")
        
        _state.update { 
            it.copy(
                isLoading = false,
                isForceUpdateRequired = isForceUpdateRequired,
                isMaintenanceMode = maintenanceMode,
                canProceed = !isForceUpdateRequired && !maintenanceMode
            )
        }
    }

    private fun isVersionLower(currentVersion: String, requiredVersion: String): Boolean {
        return try {
            val currentParts = currentVersion.split(".").map { it.toInt() }
            val requiredParts = requiredVersion.split(".").map { it.toInt() }
            
            val maxLength = maxOf(currentParts.size, requiredParts.size)
            
            for (i in 0 until maxLength) {
                val currentPart = currentParts.getOrNull(i) ?: 0
                val requiredPart = requiredParts.getOrNull(i) ?: 0
                
                when {
                    currentPart < requiredPart -> return true
                    currentPart > requiredPart -> return false
                }
            }
            false
        } catch (e: Exception) {
            Timber.e(e, "Error comparing versions")
            false
        }
    }
} 