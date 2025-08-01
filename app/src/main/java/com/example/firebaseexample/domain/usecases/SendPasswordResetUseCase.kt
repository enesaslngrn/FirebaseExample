package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(email: String): Flow<AuthResult> {
        return authRepository.sendPasswordResetEmail(email)
    }
} 