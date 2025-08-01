package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(idToken: String): Flow<AuthResult> {
        return authRepository.signInWithGoogle(idToken)
    }
} 