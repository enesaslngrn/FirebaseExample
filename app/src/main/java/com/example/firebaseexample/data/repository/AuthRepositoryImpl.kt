package com.example.firebaseexample.data.repository

import androidx.fragment.app.FragmentActivity
import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.models.User
import com.example.firebaseexample.domain.repository.AuthRepository
import com.example.firebaseexample.data.models.UserDto
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.launch

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            emit(AuthResult.Error("Invalid credentials"))
        } catch (e: FirebaseAuthInvalidUserException) {
            emit(AuthResult.Error("User not found"))
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

    override fun deleteAccount(currentPassword: String?): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        val user = firebaseAuth.currentUser
        if (user != null) {
            try {
                val userId = user.uid
                val userProviders = user.providerData.map { it.providerId }

                if (userProviders.contains("google.com")) {
                    Timber.d("Google user detected, proceeding with account deletion")
                } else if (userProviders.contains("phone")){
                    Timber.d("Phone user detected, proceeding with account deletion")
                } else if (userProviders.contains("password") && currentPassword != null) {

                    val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
                    user.reauthenticate(credential).await()
                    Timber.d("Email/Password user reauthenticated successfully")

                } else {
                    emit(AuthResult.Error("Invalid authentication method or missing credentials"))
                    return@flow
                }
                deleteUserDataFromFirestore(userId)

                user.delete().await()

                Timber.d("Firebase Auth account deleted successfully")

                emit(AuthResult.Success(user = null))
                Timber.d("Account and user data deleted successfully")
            } catch (e: Exception) {
                Timber.e(e, "Account deletion error")
                when (e) {
                    is FirebaseAuthRecentLoginRequiredException -> {
                        val userProviders = user.providerData.map { it.providerId }
                        if (userProviders.contains("google.com")) {
                            emit(AuthResult.Error("This operation requires recent authentication. Please sign out and sign in again with Google, then try deleting your account."))
                        } else {
                            emit(AuthResult.Error("This operation requires recent authentication. Please sign in again and try again."))
                        }
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        emit(AuthResult.Error("Invalid credentials provided"))
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
                val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
                user.reauthenticate(credential).await()

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
            Timber.w(e, "Error deleting user data from Firestore: ${e.message}")
        }
    }

    override suspend fun reloadCurrentUser() {
        firebaseAuth.currentUser?.reload()?.await()
    }

    override fun verifyPhoneNumber(
        phoneNumber: String, 
        activity: FragmentActivity,
        resendToken: PhoneAuthProvider.ForceResendingToken?
    ): Flow<AuthResult> = callbackFlow {

        trySend(AuthResult.Loading)

        if (phoneNumber == "+905537414070") {
            Timber.d("Test phone number detected")
            firebaseAuth.firebaseAuthSettings.apply {
                setAppVerificationDisabledForTesting(true)
                forceRecaptchaFlowForTesting(false)
                setAutoRetrievedSmsCodeForPhoneNumber(phoneNumber, "123456")
            }
            Timber.d("App verification disabled for testing")
        }
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Timber.d("Phone verification completed automatically")
                launch {
                    try {
                        val result = firebaseAuth.signInWithCredential(credential).await()
                        result.user?.let { user ->
                            Timber.d("Auto sign-in successful for user: ${user.uid}")
                            trySend(AuthResult.Success(user.toUserDto().toDomain()))
                        } ?: trySend(AuthResult.Error("Auto sign-in failed"))
                    } catch (e: Exception) {
                        Timber.e(e, "Auto sign-in failed")
                        trySend(AuthResult.Error(e.message ?: "Auto sign-in failed"))
                    }
                    close()
                }
            }
            override fun onVerificationFailed(p0: FirebaseException) {
                Timber.e(p0, "Phone verification failed")
                when (p0) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        trySend(AuthResult.Error("Invalid phone number"))
                    }
                    is FirebaseTooManyRequestsException -> {
                        trySend(AuthResult.Error("Too many requests, please try again later"))
                    }
                    is FirebaseAuthMissingActivityForRecaptchaException -> {
                        trySend(AuthResult.Error("Missing activity for reCAPTCHA verification"))
                    }
                    else -> {
                        trySend(AuthResult.Error(p0.message ?: "Phone verification failed"))
                    }
                }
                close()
            }
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                val action = if (resendToken != null) "resent to" else "sent to"
                Timber.d("Verification code $action $phoneNumber")
                if (phoneNumber == "+905537414070") {
                    Timber.d("Test phone number - use verification code: 123456")
                }
                trySend(AuthResult.CodeSent(verificationId, token))
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                Timber.d("Code auto-retrieval timeout for verification ID: $verificationId")
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)// Auto-retrieval timeout.
            .setActivity(activity)
            .setCallbacks(callbacks)

        resendToken?.let { token ->
            optionsBuilder.setForceResendingToken(token)
            Timber.d("Using resend token for phone verification")
        }
            
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build()) // Starts verification process
        
        awaitClose { }
    }

    override fun signInWithPhoneCredential(
        verificationId: String,
        smsCode: String
    ): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
            val result = firebaseAuth.signInWithCredential(credential).await()
            result.user?.let { user ->
                emit(AuthResult.Success(user.toUserDto().toDomain()))
            } ?: emit(AuthResult.Error("Phone sign in failed"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            emit(AuthResult.Error("Invalid verification code"))
        } catch (e: Exception) {
            Timber.e(e, "Phone sign in error")
            emit(AuthResult.Error(e.message ?: "Phone sign in failed"))
        }
    }

    private fun FirebaseUser.toUserDto(): UserDto {
        return UserDto(
            id = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl.toString(),
            isEmailVerified = isEmailVerified,
            providers = providerData.map { it.providerId }
        )
    }
} 