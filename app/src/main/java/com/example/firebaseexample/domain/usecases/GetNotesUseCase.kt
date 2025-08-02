package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.models.Note
import com.example.firebaseexample.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(userId: String): Flow<List<Note>> = noteRepository.getNotes(userId)
} 