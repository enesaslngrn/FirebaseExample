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

    override fun deleteAccount(currentPassword: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        val user = firebaseAuth.currentUser
        if (user != null) {
            try {
                val userId = user.uid

                // First delete user data from Firestore
                deleteUserDataFromFirestore(userId)

                // Then reauthenticate the user with current password
                /**
                 * Hassas işlemler yapmadan önce kullanıcının kimliğinin tekrar doğrulanması (reauthentication) güvenlik gereği zorunludur.
                 * Bu yüzden credentials (kimlik bilgileri) alınıp önce reauthenticate() edilir.
                 * Change password OAuth için geçerli değildir. Sadece Email&Password girişi yapmış kullanıcılar içindir.
                 */
                val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)
                user.reauthenticate(credential).await()

                // Lastly delete the Firebase Auth account
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

    override fun verifyPhoneNumber(
        phoneNumber: String, 
        activity: FragmentActivity
    ): Flow<AuthResult> = callbackFlow {

        trySend(AuthResult.Loading)

        // Configure test phone numbers for development
        if (phoneNumber == "+905537414070") {
            Timber.d("Test phone number detected")
            firebaseAuth.firebaseAuthSettings.apply {
                //setAppVerificationDisabledForTesting(true) // Play Integrity API yada reCapthca'yı kapatır.
                //forceRecaptchaFlowForTesting(true) // Test için recaptcha'yı açar. setAppVerificationDisabledForTesting(false) olmalıdır.
                //setAutoRetrievedSmsCodeForPhoneNumber(phoneNumber, "123456") // Auto retrieve testi için kullanılır.
            }
            Timber.d("App verification disabled for testing")
        }
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Timber.d("Phone verification completed automatically")
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
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
                Timber.d("Verification code sent to $phoneNumber" )
                // Bu sadece manuel giriş olacaksa yani auto retrieve yok ise tetiklenir.
                // Burada verificationId ve token'ı bir yerde saklıyoruz. Bu sayede resend etmek için kullanacağız.
                trySend(AuthResult.CodeSent(verificationId, token))
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                Timber.d("Code auto-retrieval timeout for verification ID: $verificationId")
                // SMS geldikten sonra, sistemin otomatik doldurması için belirlenen timeout süresi.
                // This is called when the timeout expires and auto-retrieval is no longer possible
                // The user must manually enter the verification code
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
            
        PhoneAuthProvider.verifyPhoneNumber(options) // Starts verification process
        
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

    override fun resendVerificationCode(
        phoneNumber: String,
        activity: FragmentActivity,
        resendToken: PhoneAuthProvider.ForceResendingToken
    ): Flow<AuthResult> = callbackFlow {
        trySend(AuthResult.Loading)
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Timber.d("Phone verification completed on resend")
                // Directly sign in with the credential
                launch {
                    try {
                        val result = firebaseAuth.signInWithCredential(credential).await()
                        result.user?.let { user ->
                            Timber.d("Resend auto sign-in successful for user: ${user.uid}")
                            trySend(AuthResult.Success(user.toUserDto().toDomain()))
                        } ?: trySend(AuthResult.Error("Resend auto sign-in sign in failed"))
                    } catch (e: Exception) {
                        Timber.e(e, "Resend auto sign-in failed")
                        trySend(AuthResult.Error(e.message ?: "Phone sign in failed"))
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
                Timber.d("Verification code resent to $phoneNumber")
                if (phoneNumber == "+905537414070") {
                    Timber.d("Test phone number - use verification code: 123456")
                }
                trySend(AuthResult.CodeSent(verificationId, token))
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                Timber.d("Code auto-retrieval timeout on resend for verification ID: $verificationId")
            }
        }

        // For resend, we need to use the ForceResendingToken
        // In a real implementation, you'd store the actual token object
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(resendToken)
            .build()
            
        PhoneAuthProvider.verifyPhoneNumber(options)
        
        awaitClose { /* cleanup if needed */ }
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