package com.example.firebaseexample.presentation.notes

import com.example.firebaseexample.domain.models.Note

data class NoteState(
    val notes: List<Note> = emptyList(),
    val selectedNote: Note? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
) 