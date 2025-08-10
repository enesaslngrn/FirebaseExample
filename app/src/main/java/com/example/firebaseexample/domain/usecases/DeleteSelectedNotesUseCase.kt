package com.example.firebaseexample.domain.usecases

import com.example.firebaseexample.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeleteSelectedNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(userId: String, noteIds: List<String>): Flow<Result<Unit>> {
        return noteRepository.deleteSelectedNotes(userId, noteIds)
    }
} 