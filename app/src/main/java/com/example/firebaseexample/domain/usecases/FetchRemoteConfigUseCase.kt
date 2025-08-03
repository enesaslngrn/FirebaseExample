package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.RemoteConfigResult
import com.example.firebaseexample.domain.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchRemoteConfigUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository
) {
    operator fun invoke(): Flow<RemoteConfigResult> {
        return remoteConfigRepository.fetchRemoteConfig()
    }
} 