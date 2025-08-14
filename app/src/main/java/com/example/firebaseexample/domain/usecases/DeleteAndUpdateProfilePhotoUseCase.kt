package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.repository.AuthRepository
import com.example.firebaseexample.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class DeleteAndUpdateProfilePhotoUseCase @Inject constructor(
    private val storageRepository: StorageRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(userId: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        
        try {
            storageRepository.deleteUserProfilePhoto(userId).collect { storageResult ->
                storageResult.fold(
                    onSuccess = {
                        Timber.d("Photo deleted from storage successfully")
                        authRepository.updateUserProfilePhoto(null).collect { authResult ->
                            emit(authResult)
                        }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Failed to delete photo from storage")
                        emit(AuthResult.Error(exception.message ?: "Failed to delete photo"))
                    }
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during profile photo deletion")
            emit(AuthResult.Error(e.message ?: "Unexpected error"))
        }
    }
} 