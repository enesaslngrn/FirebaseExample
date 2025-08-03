package com.example.firebaseexample.domain.repository

import com.example.firebaseexample.domain.models.RemoteConfigResult
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun fetchRemoteConfig(): Flow<RemoteConfigResult>
    fun getForceUpdateVersion(): String
    fun isMaintenanceMode(): Boolean
} 