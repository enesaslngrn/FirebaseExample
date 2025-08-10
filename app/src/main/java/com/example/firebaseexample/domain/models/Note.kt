package com.example.firebaseexample.domain.models

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val updatedAt: Long? = null,
    val attachmentUrl: String? = null
) 