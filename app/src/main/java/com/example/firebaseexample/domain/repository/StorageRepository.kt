package com.example.firebaseexample.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun uploadUserProfilePhoto(userId: String, fileUri: Uri): Flow<Result<String>>
    fun deleteUserProfilePhoto(userId: String): Flow<Result<Unit>>

    fun uploadNoteAttachment(userId: String, noteId: String, fileUri: Uri): Flow<Result<String>>
    fun getNoteAttachmentUrl(userId: String, noteId: String): Flow<Result<String?>>
    fun deleteNoteAttachment(userId: String, noteId: String): Flow<Result<Unit>>
} 