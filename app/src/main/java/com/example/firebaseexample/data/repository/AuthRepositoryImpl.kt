package com.example.firebaseexample.data.repository

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.models.User
import com.example.firebaseexample.domain.repository.AuthRepository
import com.example.firebaseexample.data.models.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        /**
         * Bu yapı, callback tabanlı API’leri Kotlin Flow’a çevirmek için kullanılır.
         * FirebaseAuth.AuthStateListener bir callback olduğu için callbackFlow doğru seçimdir.
         * trySend(user) - Listener tetiklendiğinde yeni kullanıcı durumu Flow'a gönderilir. Bu sayede collect {} bloğunda UI otomatik güncellenebilir.
         * awaitClose - Flow iptal edildiğinde (örneğin fragment destroy olduğunda) dinleyiciyi kaldırır. Bellek sızıntısı (memory leak) yaşanmaması için çok önemlidir.
         */
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser?.toUserDto()?.toDomain()
            trySend(user)
        }
        
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun signInWithEmailAndPassword(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                emit(AuthResult.Success(user.toUserDto().toDomain()))
            } ?: emit(AuthResult.Error("Sign in failed"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            emit(AuthResult.Error("Invalid credentials"))
        } catch (e: FirebaseAuthInvalidUserException) {
            emit(AuthResult.Error("User not found"))
        } catch (e: Exception) {
            Timber.e(e, "Sign in error")
            emit(AuthResult.Error(e.message ?: "Sign in failed"))
        }
    }

//    override fun signInWithEmailAndPassword(email: String, password: String): Flow<AuthResult> = callbackFlow {
//        trySend(AuthResult.Loading)
//
//        val task = firebaseAuth.signInWithEmailAndPassword(email, password)
//        task.addOnCompleteListener { taskResult ->
//            if (taskResult.isSuccessful) {
//                val user = taskResult.result?.user
//                if (user != null) {
//                    trySend(AuthResult.Success(user.toUserDto().toDomain()))
//                } else {
//                    trySend(AuthResult.Error("Sign in failed"))
//                }
//            } else {
//                val error = taskResult.exception?.message ?: "Sign in failed"
//                Timber.e(taskResult.exception, "Sign in error")
//                trySend(AuthResult.Error(error))
//            }
//            close()
//        }
//
//        awaitClose { /* no active listener to remove in this case */ }
//    }

//    override fun signInWithEmailAndPassword(email: String, password: String): Flow<AuthResult> = callbackFlow {
//        trySend(AuthResult.Loading)
//
//        val task = firebaseAuth.signInWithEmailAndPassword(email, password)
//        task.addOnSuccessListener { result ->
//            val user = result.user
//            if (user != null) {
//                trySend(AuthResult.Success(user.toUserDto().toDomain()))
//            } else {
//                trySend(AuthResult.Error("Sign in failed"))
//            }
//            close()
//        }.addOnFailureListener { exception ->
//            Timber.e(exception, "Sign in error")
//            trySend(AuthResult.Error(exception.message ?: "Sign in failed"))
//            close()
//        }
//
//        awaitClose { /* clean-up if needed */ }
//    }


    override fun signUpWithEmailAndPassword(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                emit(AuthResult.Success(user.toUserDto().toDomain()))
            } ?: emit(AuthResult.Error("Sign up failed"))
        } catch (e: Exception) {
            Timber.e(e, "Sign up error")
            emit(AuthResult.Error(e.message ?: "Sign up failed"))
        }
    }

    override fun signOut(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            firebaseAuth.signOut()
            emit(AuthResult.Success(user = null))
        } catch (e: Exception) {
            Timber.e(e, "Sign out error")
            emit(AuthResult.Error(e.message ?: "Sign out failed"))
        }
    }

    override fun sendPasswordResetEmail(email: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            emit(AuthResult.Success(user = null))
        } catch (e: Exception) {
            Timber.e(e, "Password reset error")
            emit(AuthResult.Error(e.message ?: "Password reset failed"))
        }
    }

    override fun signInWithGoogle(idToken: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let { user ->
                emit(AuthResult.Success(user.toUserDto().toDomain()))
            } ?: emit(AuthResult.Error("Google sign in failed"))
        } catch (e: Exception) {
            Timber.e(e, "Google sign in error")
            emit(AuthResult.Error(e.message ?: "Google sign in failed"))
        }
    }

    override fun sendEmailVerification(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        val user = firebaseAuth.currentUser
        if (user != null) {
            try {
                user.sendEmailVerification().await()
                emit(AuthResult.Success(user.toUserDto().toDomain()))
            } catch (e: Exception) {
                emit(AuthResult.Error(e.message ?: "Failed to send verification email"))
            }
        } else {
            emit(AuthResult.Error("No user logged in"))
        }
    }

    override fun deleteAccount(): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        val user = firebaseAuth.currentUser
        if (user != null) {
            try {
                val userId = user.uid
                
                // First delete user data from Firestore
                deleteUserDataFromFirestore(userId)

                // Then delete the Firebase Auth account
                user.delete().await()
                
                emit(AuthResult.Success(user = null))
                Timber.d("Account deleted successfully")
            } catch (e: Exception) {
                Timber.e(e, "Account deletion error")
                when (e) {
                    is FirebaseAuthRecentLoginRequiredException -> {
                        emit(AuthResult.Error("This operation is sensitive and requires recent authentication."))
                    }
                    else -> {
                        emit(AuthResult.Error(e.message ?: "Failed to delete account"))
                    }
                }
            }
        } else {
            emit(AuthResult.Error("No user logged in"))
        }
    }

    override fun changePassword(currentPassword: String, newPassword: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        val user = firebaseAuth.currentUser
        if (user != null) {
            try {
                // First reauthenticate the user with current password
                /**
                 * Hassas işlemler yapmadan önce kullanıcının kimliğinin tekrar doğrulanması (reauthentication) güvenlik gereği zorunludur.
                 * Bu yüzden credentials (kimlik bilgileri) alınıp önce reauthenticate() edilir.
                 * Change password OAuth için geçerli değildir. Sadece Email&Password girişi yapmış kullanıcılar içindir.
                 */
                val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
                user.reauthenticate(credential).await()
                
                // Then update password
                user.updatePassword(newPassword).await()
                emit(AuthResult.Success(user.toUserDto().toDomain()))
            } catch (e: Exception) {
                Timber.e(e, "Password change error")
                when (e) {
                    is FirebaseAuthRecentLoginRequiredException -> {
                        emit(AuthResult.Error("This operation is sensitive and requires recent authentication."))
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        emit(AuthResult.Error("Current password is incorrect"))
                    }
                    else -> {
                        emit(AuthResult.Error(e.message ?: "Failed to change password"))
                    }
                }
            }
        } else {
            emit(AuthResult.Error("No user logged in"))
        }
    }

    private suspend fun deleteUserDataFromFirestore(userId: String) {
        try {
            val notesCollection = firestore.collection("users").document(userId).collection("notes")
            val notesSnapshot = notesCollection.get().await()

            for (document in notesSnapshot.documents) {
                try {
                    document.reference.delete().await()
                    Timber.d("Deleted note: ${document.id}")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to delete note: ${document.id}")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error accessing user data in Firestore, but continuing with account deletion")
        }
    }

    override suspend fun reloadCurrentUser() {
        firebaseAuth.currentUser?.reload()?.await()
    }

    private fun FirebaseUser.toUserDto(): UserDto {
        return UserDto(
            id = uid,
            email = email ?: "",
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            isEmailVerified = isEmailVerified
        )
    }
} 