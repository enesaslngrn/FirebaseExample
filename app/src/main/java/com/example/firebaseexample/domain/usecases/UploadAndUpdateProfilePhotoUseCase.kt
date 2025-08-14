package com.example.firebaseexample.domain.usecases

import android.net.Uri
import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.repository.AuthRepository
import com.example.firebaseexample.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class UploadAndUpdateProfilePhotoUseCase @Inject constructor(
    private val storageRepository: StorageRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(userId: String, fileUri: Uri): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        
        try {
            storageRepository.uploadUserProfilePhoto(userId, fileUri).collect { storageResult ->
                storageResult.fold(
                    onSuccess = { downloadUrl ->
                        Timber.d("Photo uploaded to storage successfully: $downloadUrl")
                        authRepository.updateUserProfilePhoto(downloadUrl).collect { authResult ->
                            emit(authResult)
                        }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Failed to upload photo to storage")
                        emit(AuthResult.Error(exception.message ?: "Failed to upload photo"))
                    }
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error during profile photo update")
            emit(AuthResult.Error(e.message ?: "Unexpected error"))
        }
    }
} 