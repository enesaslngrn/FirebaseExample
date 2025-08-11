package com.example.firebaseexample.data.repository

import android.net.Uri
import com.example.firebaseexample.domain.repository.StorageRepository
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val firebaseStorage: FirebaseStorage
) : StorageRepository {

    override fun uploadUserProfilePhoto(userId: String, fileUri: Uri): Flow<Result<String>> = flow {
        try {
            val ref = firebaseStorage.reference
                .child(userId)
                .child("${userId}.jpg")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()
            val uploadTask = ref.putFile(fileUri, metadata)
            val url = uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.await().toString()
            emit(Result.success(url))
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload profile photo")
            emit(Result.failure(e))
        }
    }

//    /**
//     * Yükleme durumunu simule ettiğimiz upload metodu:
//     */
//    override fun uploadUserProfilePhoto(
//        userId: String,
//        fileUri: Uri
//    ): Flow<Result<String>> = callbackFlow {
//        try {
//            val ref = firebaseStorage.reference
//                .child(userId)
//                .child("${userId}.jpg")
//
//            val metadata = StorageMetadata.Builder()
//                .setContentType("image/jpeg")
//                .build()
//
//            val uploadTask = ref.putFile(fileUri, metadata)
//
//            // 🔹 İlerleme durumunu dinle
//            uploadTask.addOnProgressListener { snapshot ->
//                val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
//                Timber.d("Yükleme ilerlemesi: %d%%", progress)
//                    if (progress in 50..75){
//                       uploadTask.pause()
//                    }
//                }
//                .addOnPausedListener {
//                    Timber.d("Yükleme duraklatıldı.")
//                    launch {
//                        delay(2000)
//                        uploadTask.resume()
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Timber.e(e, "Yükleme başarısız oldu.")
//                    trySend(Result.failure(e))
//                }
//                .addOnSuccessListener {
//                    Timber.d("Yükleme başarıyla tamamlandı.")
//                }
//
//            // 🔹 Yükleme tamamlanınca URL al
//            val url = uploadTask.continueWithTask { task ->
//                if (!task.isSuccessful) {
//                    task.exception?.let { throw it }
//                }
//                ref.downloadUrl
//            }.await().toString()
//
//            trySend(Result.success(url))
//            awaitClose {  }
//        } catch (e: Exception) {
//            Timber.e(e, "Profil fotoğrafı yükleme hatası")
//            trySend(Result.failure(e))
//        }
//    }

    override fun getUserProfilePhotoUrl(userId: String): Flow<Result<String?>> = flow {
        try {
            val ref = firebaseStorage.reference
                .child(userId)
                .child(userId)
            val url = ref.downloadUrl.await().toString()
            emit(Result.success(url))
        } catch (e: Exception) {
            Timber.w(e, "Profile photo not found or failed to get url")
            emit(Result.success(null))
        }
    }

    override fun deleteUserProfilePhoto(userId: String): Flow<Result<Unit>> = flow {
        try {
            val ref = firebaseStorage.reference
                .child(userId)
                .child(userId)
            ref.delete().await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete profile photo")
            emit(Result.failure(e))
        }
    }

    override fun uploadNoteAttachment(userId: String, noteId: String, fileUri: Uri): Flow<Result<String>> = flow {
        try {
            val ref = firebaseStorage.reference
                .child(userId)
                .child("notes")
                .child("${noteId}.jpg")
            ref.putFile(fileUri).await()
            val url = ref.downloadUrl.await().toString()
            emit(Result.success(url))
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload note attachment")
            emit(Result.failure(e))
        }
    }

    override fun getNoteAttachmentUrl(userId: String, noteId: String): Flow<Result<String?>> = flow {
        try {
            val ref = firebaseStorage.reference
                .child(userId)
                .child("notes")
                .child("${noteId}.jpg")
            val url = ref.downloadUrl.await().toString()
            emit(Result.success(url))
        } catch (e: Exception) {
            Timber.w(e, "Note attachment not found or failed to get url")
            emit(Result.success(null))
        }
    }

    override fun deleteNoteAttachment(userId: String, noteId: String): Flow<Result<Unit>> = flow {
        try {
            val ref = firebaseStorage.reference
                .child(userId)
                .child("notes")
                .child("${noteId}.jpg")
            ref.delete().await()
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete note attachment")
            emit(Result.failure(e))
        }
    }
} 