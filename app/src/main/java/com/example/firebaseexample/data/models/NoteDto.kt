package com.example.firebaseexample.data.models

import com.example.firebaseexample.domain.models.Note

data class NoteDto(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val updatedAt: Long? = null
) {
    fun toDomain() = Note(id, title, content, timestamp, updatedAt)
    companion object {
        fun fromDomain(note: Note) = NoteDto(
            id = note.id,
            title = note.title,
            content = note.content,
            timestamp = note.timestamp,
            updatedAt = note.updatedAt
        )
    }
} 