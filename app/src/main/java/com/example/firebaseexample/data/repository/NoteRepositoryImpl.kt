package com.example.firebaseexample.data.repository

import com.example.firebaseexample.data.models.NoteDto
import com.example.firebaseexample.domain.models.Note
import com.example.firebaseexample.domain.repository.NoteRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NoteRepository {
    override fun getNotes(userId: String): Flow<List<Note>> = callbackFlow {
        val ref = firestore.collection("users").document(userId).collection("notes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
        val listener = ref.addSnapshotListener { snapshot, _ ->
            val notes = snapshot?.documents?.mapNotNull { it.toObject(NoteDto::class.java)?.toDomain() } ?: emptyList()
            trySend(notes)
        }
        awaitClose { listener.remove() }
    }

    override fun addNote(userId: String, note: Note): Flow<Result<Unit>> = flow {
        try {
            val ref = firestore.collection("users").document(userId).collection("notes")
            val noteId = note.id.ifBlank { ref.document().id }
            val noteDto = NoteDto.fromDomain(note.copy(id = noteId))
            ref.document(noteId).set(noteDto).await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun updateNote(userId: String, note: Note): Flow<Result<Unit>> = flow {
        try {
            val ref = firestore.collection("users").document(userId).collection("notes")
            val noteDto = NoteDto.fromDomain(note.copy(updatedAt = System.currentTimeMillis()))
            ref.document(note.id).set(noteDto).await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun deleteNote(userId: String, noteId: String): Flow<Result<Unit>> = flow {
        try {
            val ref = firestore.collection("users").document(userId).collection("notes")
            ref.document(noteId).delete().await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getNote(userId: String, noteId: String): Flow<Note?> = flow {
        try {
            val ref = firestore.collection("users").document(userId).collection("notes")
            val snapshot = ref.document(noteId).get().await()
            emit(snapshot.toObject(NoteDto::class.java)?.toDomain())
        } catch (e: Exception) {
            emit(null)
        }
    }
} 