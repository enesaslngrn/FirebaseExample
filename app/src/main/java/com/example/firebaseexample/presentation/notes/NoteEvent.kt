package com.example.firebaseexample.presentation.notes

import com.example.firebaseexample.domain.models.Note

sealed class NoteEvent {
    data object LoadNotes : NoteEvent()
    data class AddNote(val title: String, val content: String) : NoteEvent()
    data class UpdateNote(val note: Note) : NoteEvent()
    data class DeleteNote(val noteId: String) : NoteEvent()
    data object DeleteAllNotes : NoteEvent()
    data class DeleteSelectedNotes(val noteIds: List<String>) : NoteEvent()
    data class SelectNote(val note: Note) : NoteEvent()
    data class ToggleNoteSelection(val note: Note) : NoteEvent()
    data object ClearSelectedNote : NoteEvent()
    data object ClearSelectedNotes : NoteEvent()
    data object EnterSelectionMode : NoteEvent()
    data object ExitSelectionMode : NoteEvent()
    data object ClearError : NoteEvent()
    data object ClearSuccessMessage : NoteEvent()
} 