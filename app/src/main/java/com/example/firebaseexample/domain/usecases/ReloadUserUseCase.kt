package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.repository.AuthRepository
import javax.inject.Inject

class ReloadUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.reloadCurrentUser()
    }
} 