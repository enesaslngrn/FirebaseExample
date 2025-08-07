package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignInWithPhoneUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(verificationId: String, smsCode: String): Flow<AuthResult> {
        return authRepository.signInWithPhoneCredential(verificationId, smsCode)
    }
} 