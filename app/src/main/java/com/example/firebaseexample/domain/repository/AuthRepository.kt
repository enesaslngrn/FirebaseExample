package com.example.firebaseexample.domain.repository

import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.models.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    fun signInWithEmailAndPassword(email: String, password: String): Flow<AuthResult>
    fun signUpWithEmailAndPassword(email: String, password: String): Flow<AuthResult>
    fun signOut(): Flow<AuthResult>
    fun sendPasswordResetEmail(email: String): Flow<AuthResult>
} 