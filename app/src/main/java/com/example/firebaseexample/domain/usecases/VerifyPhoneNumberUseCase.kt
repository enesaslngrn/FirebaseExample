package com.example.firebaseexample.domain.usecases

import androidx.fragment.app.FragmentActivity
import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.repository.AuthRepository
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class VerifyPhoneNumberUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(
        phoneNumber: String, 
        activity: FragmentActivity,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null
    ): Flow<AuthResult> {
        return authRepository.verifyPhoneNumber(phoneNumber, activity, resendToken)
    }
} 