package com.example.firebaseexample.presentation.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.usecases.GetCurrentUserUseCase
import com.example.firebaseexample.domain.usecases.SendPasswordResetUseCase
import com.example.firebaseexample.domain.usecases.SignInUseCase
import com.example.firebaseexample.domain.usecases.SignOutUseCase
import com.example.firebaseexample.domain.usecases.SignUpUseCase
import com.example.firebaseexample.domain.usecases.SignInWithGoogleUseCase
import com.example.firebaseexample.domain.usecases.SendEmailVerificationUseCase
import com.example.firebaseexample.domain.usecases.DeleteAccountUseCase
import com.example.firebaseexample.domain.usecases.ReloadUserUseCase
import com.example.firebaseexample.domain.usecases.ChangePasswordUseCase
import com.example.firebaseexample.domain.usecases.VerifyPhoneNumberUseCase
import com.example.firebaseexample.domain.usecases.SignInWithPhoneUseCase
import com.example.firebaseexample.domain.usecases.UploadAndUpdateProfilePhotoUseCase
import com.example.firebaseexample.domain.usecases.DeleteAndUpdateProfilePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val sendEmailVerificationUseCase: SendEmailVerificationUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val reloadUserUseCase: ReloadUserUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val verifyPhoneNumberUseCase: VerifyPhoneNumberUseCase,
    private val signInWithPhoneUseCase: SignInWithPhoneUseCase,
    private val uploadAndUpdateProfilePhotoUseCase: UploadAndUpdateProfilePhotoUseCase,
    private val deleteAndUpdateProfilePhotoUseCase: DeleteAndUpdateProfilePhotoUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private companion object {
        const val VERIFICATION_TIMEOUT_SECONDS = 30
    }

    init {
        viewModelScope.launch {
            try {
                reloadUserUseCase()
                Timber.d("Initial user reload completed on app start")
            } catch (e: Exception) {
                Timber.w(e, "Initial user reload failed, continuing with cached data")
            }
            observeCurrentUser()
        }
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SignIn -> signIn(event.email, event.password)
            is AuthEvent.SignUp -> signUp(event.email, event.password)
            is AuthEvent.SignOut -> signOut()
            is AuthEvent.SendPasswordReset -> sendPasswordReset(event.email)
            is AuthEvent.SignInWithGoogle -> signInWithGoogle(event.idToken)
            is AuthEvent.SendEmailVerification -> sendEmailVerification()
            is AuthEvent.DeleteAccount -> deleteAccount(event.currentPassword)
            is AuthEvent.ClearError -> _state.update { it.copy(error = null) }
            is AuthEvent.ClearSuccessMessage -> _state.update { it.copy(successMessage = null, accountDeleted = false) }
            is AuthEvent.ChangePassword -> changePassword(event.currentPassword, event.newPassword)

            // Profile photo events
            is AuthEvent.UploadProfilePhoto -> uploadProfilePhoto(event.userId, event.fileUri)
            is AuthEvent.DeleteProfilePhoto -> deleteProfilePhoto(event.userId)

            // Phone Auth events
            is AuthEvent.TogglePhoneAuthMode -> togglePhoneAuthMode()
            is AuthEvent.VerifyPhoneNumber -> verifyPhoneNumber(event.phoneNumber, event.fragmentActivity)
            is AuthEvent.VerifySmsCode -> verifySmsCode(event.smsCode)
            is AuthEvent.ResendVerificationCode -> resendVerificationCode(event.fragmentActivity)
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _state.update {
                    it.copy(
                        user = user,
                        isInitialized = true
                    )
                }
            }
        }
    }

    private fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            signInUseCase(email, password).collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.Success -> _state.update { 
                        it.copy(isLoading = false, user = result.user, error = null)
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Sign in error: ${result.message}")
                    }
                    is AuthResult.CodeSent -> { }
                }
            }
        }
    }

    private fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            signUpUseCase(email, password).collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.Success -> _state.update { 
                        it.copy(isLoading = false, user = result.user, error = null)
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Sign up error: ${result.message}")
                    }
                    is AuthResult.CodeSent -> {}
                }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            signOutUseCase().collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.Success -> _state.update { 
                        it.copy(isLoading = false, user = null, error = null)
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Sign out error: ${result.message}")
                    }
                    is AuthResult.CodeSent -> {}
                }
            }
        }
    }

    private fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            sendPasswordResetUseCase(email).collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.Success -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = null,
                                successMessage = "Password reset email sent successfully"
                            )
                        }
                        Timber.d("Password reset email sent successfully")
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Password reset error: ${result.message}")
                    }
                    is AuthResult.CodeSent -> { }
                }
            }
        }
    }

    private fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            signInWithGoogleUseCase(idToken).collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.Success -> {
                        _state.update {
                            it.copy(isLoading = false, user = result.user, error = null)
                        }
                        Timber.d("Google sign in successful: ${result.user?.email}")
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Google sign in error: ${result.message}")
                    }
                    is AuthResult.CodeSent -> { }
                }
            }
        }
    }

    private fun sendEmailVerification() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            sendEmailVerificationUseCase().collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.Success ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                verificationMessage = "Verification email sent"
                            )
                        }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Email verification error: ${result.message}")
                    }
                    is AuthResult.CodeSent -> { }
                }
            }
        }
    }

    private fun deleteAccount(currentPassword: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            deleteAccountUseCase(currentPassword).collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.Success -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                user = null,
                                error = null,
                                successMessage = "Account deleted successfully",
                                accountDeleted = true
                            )
                        }
                        Timber.d("Account deleted successfully")
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Account deletion error: ${result.message}")
                    }
                    is AuthResult.CodeSent -> { }
                }
            }
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            changePasswordUseCase(currentPassword, newPassword).collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.Success -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = null,
                                successMessage = "Password changed successfully"
                            )
                        }
                        Timber.d("Password changed successfully")
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Password change error: ${result.message}")
                    }
                    is AuthResult.CodeSent -> { }
                }
            }
        }
    }

    fun reloadCurrentUser(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                reloadUserUseCase()
                Timber.d("User reloaded successfully - state will be updated by observer")
            } catch (e: Exception) {
                Timber.w(e, "Failed to reload user")
            }
            onComplete?.invoke()
        }
    }

    private fun togglePhoneAuthMode() {
        _state.update { 
            it.copy(
                isPhoneAuthMode = !it.isPhoneAuthMode,
                error = null,
                successMessage = null
            ) 
        }
    }

    private fun verifyPhoneNumber(phoneNumber: String, activity: FragmentActivity) {
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    phoneNumber = phoneNumber,
                    isLoading = true,
                    error = null,
                    successMessage = null
                ) 
            }
            
            verifyPhoneNumberUseCase(phoneNumber, activity).collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.CodeSent -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                verificationId = result.verificationId,
                                resendToken = result.resendToken,
                                isCodeSent = true,
                                successMessage = "Verification code sent to $phoneNumber"
                            )
                        }
                        Timber.d("SMS verification code sent")
                        Timber.d("VerificationId: ${result.verificationId}")
                        if (phoneNumber == "+905537414070") { Timber.d("Test phone number - use code: 123456") }

                        startVerificationTimer()
                    }
                    is AuthResult.Success -> {
                        stopTimer()
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                user = result.user,
                                error = null,
                                successMessage = "Phone sign-in successful",
                                isPhoneAuthMode = false,
                                isCodeSent = false
                            )
                        }
                        Timber.d("Phone verification and sign-in completed successfully")
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Phone verification error: ${result.message}")
                    }
                }
            }
        }
    }

    private fun verifySmsCode(smsCode: String) {
        val currentState = _state.value
        if (currentState.verificationId.isEmpty()) {
            _state.update { it.copy(error = "No verification ID available") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isVerifyingCode = true, error = null) }
            
            signInWithPhoneUseCase(currentState.verificationId, smsCode).collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isVerifyingCode = true) }
                    is AuthResult.Success -> {
                        _state.update { 
                            it.copy(
                                isVerifyingCode = false,
                                user = result.user,
                                error = null,
                                successMessage = "Phone sign in successful",
                                isPhoneAuthMode = false,
                                isCodeSent = false
                            )
                        }
                        Timber.d("Phone sign in successful")
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isVerifyingCode = false, error = result.message) }
                        Timber.e("SMS verification error: ${result.message}")
                    }
                    else -> { }
                }
            }
        }
    }

    private fun resendVerificationCode(activity: FragmentActivity) {
        val currentState = _state.value
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    isCodeExpired = false,
                    error = null
                )
            }
            currentState.resendToken?.let { resendToken ->
                verifyPhoneNumberUseCase(currentState.phoneNumber, activity, resendToken).collect { result ->
                    when (result) {
                        is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                        is AuthResult.CodeSent -> {
                            _state.update { 
                                it.copy(
                                    isLoading = false,
                                    verificationId = result.verificationId,
                                    resendToken = result.resendToken,
                                    successMessage = "Verification code resent",
                                    isCodeSent = true
                                )
                            }
                            Timber.d("SMS verification code resent")
                            startVerificationTimer()
                        }
                        is AuthResult.Success -> {
                            stopTimer()
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    user = result.user,
                                    error = null,
                                    successMessage = "Phone sign-in successful",
                                    isPhoneAuthMode = false,
                                    isCodeSent = false
                                )
                            }
                            Timber.d("Resend phone verification and sign-in completed successfully")
                        }
                        is AuthResult.Error -> {
                            _state.update { it.copy(isLoading = false, error = result.message) }
                            Timber.e("Resend verification error: ${result.message}")
                        }
                    }
                }
            }
        }
    }

    private fun startVerificationTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remainingSeconds = VERIFICATION_TIMEOUT_SECONDS
            _state.update { 
                it.copy(
                    remainingTime = remainingSeconds,
                    isTimerActive = true,
                    isCodeExpired = false
                )
            }
            
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                _state.update { it.copy(remainingTime = remainingSeconds) }
            }

            handleTimeoutExpired()
        }
    }

    private fun handleTimeoutExpired() {
        _state.update { 
            it.copy(
                remainingTime = 0,
                error = "Verification code expired. Please request a new code.",
                isCodeExpired = true,
                isTimerActive = false
            )
        }
        Timber.d("Verification code expired")
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _state.update { 
            it.copy(
                isTimerActive = false,
                remainingTime = 0
            )
        }
    }

    private fun uploadProfilePhoto(userId: String, fileUri: android.net.Uri) {
        viewModelScope.launch {
            uploadAndUpdateProfilePhotoUseCase(userId, fileUri).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                    is AuthResult.Success -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                successMessage = "Profile photo updated successfully"
                            ) 
                        }
                    }
                    is AuthResult.Error -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = result.message
                            ) 
                        }
                        Timber.e("Failed to update profile photo: ${result.message}")
                    }
                    else -> { }
                }
            }
        }
    }

    private fun deleteProfilePhoto(userId: String) {
        viewModelScope.launch {
            deleteAndUpdateProfilePhotoUseCase(userId).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                    is AuthResult.Success -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                successMessage = "Profile photo deleted successfully"
                            ) 
                        }
                    }
                    is AuthResult.Error -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = result.message
                            ) 
                        }
                        Timber.e("Failed to delete profile photo: ${result.message}")
                    }
                    else -> { }
                }
            }
        }
    }
} 