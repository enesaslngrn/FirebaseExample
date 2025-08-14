package com.example.firebaseexample.presentation.home

import android.net.Uri

sealed class HomeStorageEvent {
    data object LoadProfilePhoto : HomeStorageEvent()
    data class UploadProfilePhoto(val fileUri: Uri) : HomeStorageEvent()
    data object DeleteProfilePhoto : HomeStorageEvent()
} 