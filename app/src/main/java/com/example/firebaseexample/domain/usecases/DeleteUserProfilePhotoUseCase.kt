package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteUserProfilePhotoUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(userId: String): Flow<Result<Unit>> =
        storageRepository.deleteUserProfilePhoto(userId)
} 