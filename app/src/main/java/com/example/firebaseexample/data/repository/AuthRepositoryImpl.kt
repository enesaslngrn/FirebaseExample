package com.example.firebaseexample.data.repository

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.models.User
import com.example.firebaseexample.domain.repository.AuthRepository
import com.example.firebaseexample.data.models.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
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
        } catch (e: Exception) {
            Timber.e(e, "Sign in error")
            emit(AuthResult.Error(e.message ?: "Sign in failed"))
        }
    }

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
            emit(AuthResult.Success(User("", "", null, null, false)))
        } catch (e: Exception) {
            Timber.e(e, "Sign out error")
            emit(AuthResult.Error(e.message ?: "Sign out failed"))
        }
    }

    override fun sendPasswordResetEmail(email: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            emit(AuthResult.Success(User("", "", null, null, false)))
        } catch (e: Exception) {
            Timber.e(e, "Password reset error")
            emit(AuthResult.Error(e.message ?: "Password reset failed"))
        }
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