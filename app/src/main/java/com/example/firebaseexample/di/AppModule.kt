package com.example.firebaseexample.di

import com.example.firebaseexample.domain.repository.AuthRepository
import com.example.firebaseexample.data.repository.AuthRepositoryImpl
import com.example.firebaseexample.data.repository.NoteRepositoryImpl
import com.example.firebaseexample.domain.repository.NoteRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository {
        return authRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideNoteRepository(
        firestore: FirebaseFirestore
    ): NoteRepository = NoteRepositoryImpl(firestore)
} 