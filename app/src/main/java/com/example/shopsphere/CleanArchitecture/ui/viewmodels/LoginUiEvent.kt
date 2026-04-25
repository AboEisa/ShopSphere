package com.example.shopsphere.CleanArchitecture.ui.viewmodels

sealed class LoginUiEvent {
    data class EmailChanged(val email: String) : LoginUiEvent()
    data class PasswordChanged(val password: String) : LoginUiEvent()
    object LoginClicked : LoginUiEvent()
    data class GoogleToken(
        val idToken: String,
        val displayName: String? = null,
        val email: String? = null
    ) : LoginUiEvent()
    data class FacebookToken(val accessToken: String) : LoginUiEvent()
}
