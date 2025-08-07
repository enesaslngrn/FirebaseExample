package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.repository.AuthRepository
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ResendVerificationCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(phoneNumber: String, activity: androidx.fragment.app.FragmentActivity, resendToken: PhoneAuthProvider.ForceResendingToken): Flow<AuthResult> {
        return authRepository.resendVerificationCode(phoneNumber, activity, resendToken)
    }
} 