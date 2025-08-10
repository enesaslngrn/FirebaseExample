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
import timber.log.Timber
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NoteRepository {
    override fun getNotes(userId: String): Flow<List<Note>> = callbackFlow {
        val ref = firestore.collection("users").document(userId).collection("notes")
            .orderBy("timestamp", Query.Direction.DESCENDING)
        val listener = ref.addSnapshotListener { snapshot, _ ->
            val notes = snapshot?.documents?.mapNotNull {
                it.toObject(NoteDto::class.java)?.toDomain() } ?: emptyList()
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
            //ref.add(noteDto).await() // Eğer authentication'dan gelen uid yok ise otomatik document id ile oluşturur.
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun updateNote(userId: String, note: Note): Flow<Result<Unit>> = flow {
        try {
            val ref = firestore.collection("users").document(userId).collection("notes")
            val noteDto = NoteDto.fromDomain(note.copy(updatedAt = System.currentTimeMillis()))
            //val noteDtoSet = NoteDto(title = note.title, content = note.content) // Eğer set kullanacaksak NoteDto hep dolu olmalı.
            ref.document(note.id).set(noteDto).await()

            //val updatedFields = mapOf("title" to note.title, "content" to note.content) // Sadece update edilecek alan dolu olsa yeterli.
            //ref.document(note.id).update(updatedFields).await()

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

    /**
     * Kullanıcının tüm notlarını batch işlemi ile siler
     * 
     * BATCH YAKLAŞIMI:
     * - Önce tüm dökümanları getirir
     * - Batch işlemi ile hepsini "atomik" olarak siler
     * - Ya hepsi silinir ya hiçbiri silinmez
     * - Tek network çağrısı ile performanslı silme
     */
    override fun deleteAll(userId: String): Flow<Result<Unit>> = flow {
        try {
            val ref = firestore.collection("users").document(userId).collection("notes")

            Timber.d("Starting batch deletion of all notes for user: $userId")
            
            // Önce tüm dökümanları getir
            val snapshot = ref.get().await()
            val documents = snapshot.documents
            
            if (documents.isEmpty()) {
                Timber.d("No notes found to delete for user: $userId")
                emit(Result.success(Unit))
                return@flow
            }

            // Batch işlemi ile tümünü sil - Ama 2. indexte hata simülasyonu yapalım
            firestore.runBatch {
                batch -> documents.forEachIndexed { index, documentSnapshot ->
                    if (index == 2) {
                        throw Exception("Batch deletion failed")
                    }
                    batch.delete(documentSnapshot.reference)
                }
            }.await()

            Timber.d("Successfully deleted all ${documents.size} notes for user: $userId")
            emit(Result.success(Unit))
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete all notes for user: $userId")
            emit(Result.failure(e))
        }
    }

    /**
     * Seçili notları döküman bazlı tek tek siler
     * 
     * BİREYSEL YAKLAŞIM:
     * - Her döküman için ayrı delete() çağrısı yapar
     * - Bir işlem başarısız olsa bile diğerleri devam eder
     * - Kısmi başarı durumları olabilir
     * - Her işlem için ayrı network çağrısı gerekir
     */
    override fun deleteSelectedNotes(userId: String, noteIds: List<String>): Flow<Result<Unit>> = flow {
        if (noteIds.isEmpty()) {
            emit(Result.success(Unit))
            return@flow
        }

        try {
            val ref = firestore.collection("users").document(userId).collection("notes")
            val results = mutableListOf<Result<Unit>>()
            
            Timber.d("Starting individual deletion for ${noteIds.size} selected notes")
            
            // Her notu tek tek sil - Ama 2. indexte hata simülasyonu yapalım
            noteIds.forEachIndexed { index, noteId ->
                if (index == 2) {
                    throw Exception("Individual deletion failed")
                }
                try {
                    ref.document(noteId).delete().await()
                    results.add(Result.success(Unit))
                    Timber.d("Successfully deleted note: $noteId")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to delete note: $noteId")
                    results.add(Result.failure(e))
                }
            }
            // Sonuçları değerlendir
            val failures = results.filter { it.isFailure }
            val successes = results.filter { it.isSuccess }
            
            Timber.d("Individual deletion completed: ${successes.size} successful, ${failures.size} failed")
            
            if (failures.isEmpty()) {
                emit(Result.success(Unit))
            } else {
                // Kısmi başarı durumunda da success döndür ama log'da belirt
                Timber.w("Some notes could not be deleted: ${failures.size} failed out of ${noteIds.size}")
                emit(Result.success(Unit)) // Kısmi başarı da success sayılır
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete selected notes")
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