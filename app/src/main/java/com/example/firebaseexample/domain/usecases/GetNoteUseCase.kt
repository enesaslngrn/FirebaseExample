package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.Note
import com.example.firebaseexample.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(userId: String, noteId: String): Flow<Note?> = noteRepository.getNote(userId, noteId)
} 