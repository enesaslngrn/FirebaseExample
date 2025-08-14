package com.example.firebaseexample.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseexample.domain.usecases.DeleteUserProfilePhotoUseCase
import com.example.firebaseexample.domain.usecases.GetUserProfilePhotoUrlUseCase
import com.example.firebaseexample.domain.usecases.UploadUserProfilePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeStorageViewModel @Inject constructor(
    private val getUserProfilePhotoUrlUseCase: GetUserProfilePhotoUrlUseCase,
    private val uploadUserProfilePhotoUseCase: UploadUserProfilePhotoUseCase,
    private val deleteUserProfilePhotoUseCase: DeleteUserProfilePhotoUseCase
) : ViewModel() {

    private val _state: MutableStateFlow<HomeStorageState> = MutableStateFlow(HomeStorageState())
    val state: StateFlow<HomeStorageState> = _state.asStateFlow()

    fun onEvent(userId: String, event: HomeStorageEvent) {
        when (event) {
            is HomeStorageEvent.LoadProfilePhoto -> loadProfilePhoto(userId)
            is HomeStorageEvent.UploadProfilePhoto -> uploadProfilePhoto(userId, event.fileUri)
            is HomeStorageEvent.DeleteProfilePhoto -> deleteProfilePhoto(userId)
        }
    }

    private fun loadProfilePhoto(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            getUserProfilePhotoUrlUseCase(userId).collect { result ->
                result.fold(
                    onSuccess = { url -> _state.update { it.copy(isLoading = false, profilePhotoUrl = url) } },
                    onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load profile photo") } }
                )
            }
        }
    }

    private fun uploadProfilePhoto(userId: String, fileUri: android.net.Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            uploadUserProfilePhotoUseCase(userId, fileUri).collect { result ->
                result.fold(
                    onSuccess = { url -> _state.update { it.copy(isLoading = false, profilePhotoUrl = url, successMessage = "Profile photo updated") } },
                    onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to upload profile photo") } }
                )
            }
        }
    }

    private fun deleteProfilePhoto(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            deleteUserProfilePhotoUseCase(userId).collect { result ->
                result.fold(
                    onSuccess = { _state.update { it.copy(isLoading = false, profilePhotoUrl = null, successMessage = "Profile photo deleted") } },
                    onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to delete profile photo") } }
                )
            }
        }
    }
} 