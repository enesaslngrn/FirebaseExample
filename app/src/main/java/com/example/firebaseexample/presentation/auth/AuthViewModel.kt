package com.example.firebaseexample.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseexample.domain.models.AuthResult
import com.example.firebaseexample.domain.usecases.GetCurrentUserUseCase
import com.example.firebaseexample.domain.usecases.SignInUseCase
import com.example.firebaseexample.domain.usecases.SignOutUseCase
import com.example.firebaseexample.domain.usecases.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        observeCurrentUser()
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SignIn -> {
                signIn(event.email, event.password)
            }
            is AuthEvent.SignUp -> {
                signUp(event.email, event.password)
            }
            is AuthEvent.SignOut -> {
                signOut()
            }
            is AuthEvent.SendPasswordReset -> {
                // TODO: Implement password reset
            }
            is AuthEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                _state.update { 
                    it.copy(
                        user = user,
                        isAuthenticated = user != null
                    )
                }
            }
        }
    }

    private fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            signInUseCase(email, password).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    is AuthResult.Success -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                user = result.user,
                                isAuthenticated = true,
                                error = null
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
                        Timber.e("Sign in error: ${result.message}")
                    }
                }
            }
        }
    }

    private fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            signUpUseCase(email, password).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    is AuthResult.Success -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                user = result.user,
                                isAuthenticated = true,
                                error = null
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
                        Timber.e("Sign up error: ${result.message}")
                    }
                }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            signOutUseCase().collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                    is AuthResult.Success -> {
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                user = null,
                                isAuthenticated = false,
                                error = null
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
                        Timber.e("Sign out error: ${result.message}")
                    }
                }
            }
        }
    }
} 