package com.example.firebaseexample.domain.repository

import com.example.firebaseexample.domain.models.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(userId: String): Flow<List<Note>>
    fun addNote(userId: String, note: Note): Flow<Result<Unit>>
    fun updateNote(userId: String, note: Note): Flow<Result<Unit>>
    fun deleteNote(userId: String, noteId: String): Flow<Result<Unit>>
    fun getNote(userId: String, noteId: String): Flow<Note?>
} 