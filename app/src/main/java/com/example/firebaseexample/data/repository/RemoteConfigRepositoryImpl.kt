package com.example.firebaseexample.data.repository

import com.example.firebaseexample.domain.models.RemoteConfigResult
import com.example.firebaseexample.domain.repository.RemoteConfigRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepositoryImpl @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig
) : RemoteConfigRepository {

    companion object {
        private const val FORCE_UPDATE_VERSION_KEY = "force_update_version"
        private const val MAINTENANCE_MODE_KEY = "maintenance_mode"
        private const val DEFAULT_FORCE_UPDATE_VERSION = "1.0.0"
        private const val DEFAULT_MAINTENANCE_MODE = false
    }

    init {
        setupRemoteConfig()
    }

    private fun setupRemoteConfig() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 60
        }
        
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val defaultValues = mapOf(
            FORCE_UPDATE_VERSION_KEY to DEFAULT_FORCE_UPDATE_VERSION,
            MAINTENANCE_MODE_KEY to DEFAULT_MAINTENANCE_MODE
        )
        firebaseRemoteConfig.setDefaultsAsync(defaultValues)
    }

    override fun fetchRemoteConfig(): Flow<RemoteConfigResult> = flow {
        try {
            emit(RemoteConfigResult.Loading)
            
            firebaseRemoteConfig.fetchAndActivate().await()
            
            val forceUpdateVersion = firebaseRemoteConfig.getString(FORCE_UPDATE_VERSION_KEY)
            val maintenanceMode = firebaseRemoteConfig.getBoolean(MAINTENANCE_MODE_KEY)
            
            Timber.d("Remote Config fetched - Force Update Version: $forceUpdateVersion, Maintenance Mode: $maintenanceMode")
            
            emit(RemoteConfigResult.Success(forceUpdateVersion, maintenanceMode))
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to fetch remote config")
            emit(RemoteConfigResult.Error(exception.message ?: "Unknown error occurred"))
        }
    }
} 