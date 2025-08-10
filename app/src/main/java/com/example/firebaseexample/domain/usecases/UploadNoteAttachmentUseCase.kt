package com.example.firebaseexample.domain.usecases

import android.net.Uri
import com.example.firebaseexample.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UploadNoteAttachmentUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(userId: String, noteId: String, fileUri: Uri): Flow<Result<String>> =
        storageRepository.uploadNoteAttachment(userId, noteId, fileUri)
} 