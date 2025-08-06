package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(currentPassword: String, newPassword: String): Flow<AuthResult> {
        return authRepository.changePassword(currentPassword, newPassword)
    }
} 