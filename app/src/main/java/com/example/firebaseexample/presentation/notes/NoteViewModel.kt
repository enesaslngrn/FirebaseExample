package com.example.firebaseexample.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseexample.domain.models.Note
import com.example.firebaseexample.domain.usecases.AddNoteUseCase
import com.example.firebaseexample.domain.usecases.DeleteNoteUseCase
import com.example.firebaseexample.domain.usecases.GetNotesUseCase
import com.example.firebaseexample.domain.usecases.UpdateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val addNoteUseCase: AddNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {

    private val _state: MutableStateFlow<NoteState> = MutableStateFlow(NoteState())
    val state: StateFlow<NoteState> = _state.asStateFlow()

    private var userId: String? = null

    fun setUserId(uid: String) {
        userId = uid
        onEvent(NoteEvent.LoadNotes)
    }

    fun onEvent(event: NoteEvent) {
        when (event) {
            is NoteEvent.LoadNotes -> loadNotes()
            is NoteEvent.AddNote -> addNote(event.title, event.content)
            is NoteEvent.UpdateNote -> updateNote(event.note)
            is NoteEvent.DeleteNote -> deleteNote(event.noteId)
            is NoteEvent.SelectNote -> selectNote(event.note)
            is NoteEvent.ClearSelectedNote -> clearSelectedNote()
            is NoteEvent.ClearError -> clearError()
            is NoteEvent.ClearSuccessMessage -> clearSuccessMessage()
        }
    }

    private fun loadNotes() {
        val uid: String = userId ?: return
        _state.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                getNotesUseCase(uid).collect { notes ->
                    _state.update { 
                        it.copy(
                            notes = notes,
                            isLoading = false,
                            isInitialized = true
                        ) 
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading notes")
                _state.update { 
                    it.copy(
                        error = "Failed to load notes",
                        isLoading = false,
                        isInitialized = true
                    ) 
                }
            }
        }
    }

    private fun addNote(title: String, content: String) {
        val uid: String = userId ?: return
        if (title.isBlank()) {
            _state.update { it.copy(error = "Title cannot be empty") }
            return
        }
        
        _state.update { it.copy(isLoading = true) }
        
        val note = Note(
            id = "",
            title = title,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            addNoteUseCase(uid, note).collect { result ->
                result.onSuccess {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Note added successfully"
                        ) 
                    }
                }.onFailure { exception ->
                    Timber.e(exception, "Error adding note")
                    _state.update { 
                        it.copy(
                            error = "Failed to add note",
                            isLoading = false
                        ) 
                    }
                }
            }
        }
    }

    private fun updateNote(note: Note) {
        val uid: String = userId ?: return
        if (note.title.isBlank()) {
            _state.update { it.copy(error = "Title cannot be empty") }
            return
        }
        
        _state.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            updateNoteUseCase(uid, note).collect { result ->
                result.onSuccess {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Note updated successfully",
                            selectedNote = null
                        ) 
                    }
                }.onFailure { exception ->
                    Timber.e(exception, "Error updating note")
                    _state.update { 
                        it.copy(
                            error = "Failed to update note",
                            isLoading = false
                        ) 
                    }
                }
            }
        }
    }

    private fun deleteNote(noteId: String) {
        val uid: String = userId ?: return
        _state.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            deleteNoteUseCase(uid, noteId).collect { result ->
                result.onSuccess {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Note deleted successfully"
                        ) 
                    }
                }.onFailure { exception ->
                    Timber.e(exception, "Error deleting note")
                    _state.update { 
                        it.copy(
                            error = "Failed to delete note",
                            isLoading = false
                        ) 
                    }
                }
            }
        }
    }

    private fun selectNote(note: Note) {
        _state.update { it.copy(selectedNote = note) }
    }

    private fun clearSelectedNote() {
        _state.update { it.copy(selectedNote = null) }
    }

    private fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }
} 