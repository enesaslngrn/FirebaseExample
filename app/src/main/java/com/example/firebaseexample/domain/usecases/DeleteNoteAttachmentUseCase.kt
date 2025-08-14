package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteNoteAttachmentUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(userId: String, noteId: String): Flow<Result<Unit>> =
        storageRepository.deleteNoteAttachment(userId, noteId)
} 