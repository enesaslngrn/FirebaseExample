package com.example.firebaseexample.data.models

import com.example.firebaseexample.domain.models.Note

data class NoteDto(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val updatedAt: Long? = null
) {
    fun toDomain(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            timestamp = timestamp,
            updatedAt = updatedAt
        )
    }
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