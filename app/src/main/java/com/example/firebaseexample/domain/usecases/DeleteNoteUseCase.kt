package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(userId: String, noteId: String): Flow<Result<Unit>> = noteRepository.deleteNote(userId, noteId)
} 