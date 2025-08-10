package com.example.firebaseexample.domain.usecases

import android.net.Uri
import com.example.firebaseexample.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UploadUserProfilePhotoUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(userId: String, fileUri: Uri): Flow<Result<String>> =
        storageRepository.uploadUserProfilePhoto(userId, fileUri)
} 