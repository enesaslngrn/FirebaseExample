package com.example.firebaseexample.data.models

import com.example.firebaseexample.domain.models.User

data class UserDto(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean?,
    val providers: List<String> = emptyList()
) {
    fun toDomain(): User {
        return User(
            id = id,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            isEmailVerified = isEmailVerified,
            providers = providers
        )
    }
    
    companion object {
        fun fromDomain(user: User): UserDto {
            return UserDto(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl,
                isEmailVerified = user.isEmailVerified,
                providers = user.providers
            )
        }
    }
} 