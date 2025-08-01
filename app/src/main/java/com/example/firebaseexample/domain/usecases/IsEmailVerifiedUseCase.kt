package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsEmailVerifiedUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Boolean> = authRepository.isEmailVerified()
} 