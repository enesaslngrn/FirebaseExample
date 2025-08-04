package com.example.firebaseexample.presentation.auth

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.flow.first

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
    private val reloadUserUseCase: ReloadUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        // Reload user once on app start to get fresh data from Firebase
        viewModelScope.launch {
            try {
                reloadUserUseCase()
                Timber.d("Initial user reload completed on app start")
            } catch (e: Exception) {
                Timber.w(e, "Initial user reload failed, continuing with cached data")
            }
            // After reload, start observing user changes
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
            is AuthEvent.DeleteAccount -> deleteAccount()
            is AuthEvent.ClearError -> _state.update { it.copy(error = null) }
            is AuthEvent.ClearSuccessMessage -> _state.update { it.copy(successMessage = null, accountDeleted = false) }
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
                        Timber.d("Google sign in successful: ${result.user.email}")
                    }
                    is AuthResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                        Timber.e("Google sign in error: ${result.message}")
                    }
                }
            }
        }
    }

    private fun sendEmailVerification() {
        viewModelScope.launch {
            sendEmailVerificationUseCase().collect { result ->
                when (result) {
                    is AuthResult.Loading -> _state.update { it.copy(isLoading = true) }
                    is AuthResult.Success -> _state.update { it.copy(isLoading = false, verificationMessage = "Verification email sent") }
                    is AuthResult.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            deleteAccountUseCase().collect { result ->
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
                }
            }
        }
    }

    fun reloadCurrentUser(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                reloadUserUseCase()
                val user = getCurrentUserUseCase().first()
                _state.update { it.copy(user = user) }
                Timber.d("User reloaded and state updated")
            } catch (e: Exception) {
                Timber.w(e, "Failed to reload user")
            }
            onComplete?.invoke()
        }
    }
} 