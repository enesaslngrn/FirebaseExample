package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class VerifyPhoneNumberUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(phoneNumber: String, activity: androidx.fragment.app.FragmentActivity): Flow<AuthResult> {
        return authRepository.verifyPhoneNumber(phoneNumber, activity)
    }
} 