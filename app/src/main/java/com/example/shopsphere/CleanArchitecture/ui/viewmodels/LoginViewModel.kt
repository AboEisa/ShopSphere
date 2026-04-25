package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.auth.FacebookLoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.GoogleLoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val facebookLoginUseCase: FacebookLoginUseCase,
    private val prefs: SharedPreference
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState

    var email: String = ""
    var password: String = ""

    fun onEvent(event: LoginUiEvent) {
        when (event) {

            is LoginUiEvent.EmailChanged -> email = event.email

            is LoginUiEvent.PasswordChanged -> password = event.password

            is LoginUiEvent.LoginClicked -> login()

            is LoginUiEvent.GoogleToken -> loginWithGoogle(event.idToken, event.displayName, event.email)

            is LoginUiEvent.FacebookToken -> loginWithFacebook(event.accessToken)
        }
    }

    private fun login() = viewModelScope.launch {
        _uiState.value = LoginUiState.Loading

        val result = loginUseCase(email, password)
        _uiState.value = if (result.isSuccess) {
            markLoggedIn()
            // Persist the email entered on the login form so the Account header
            // / My Details / Checkout can render real data without a Firebase
            // round-trip. The backend AuthResponseDto doesn't include name/email,
            // so the form input is the source of truth.
            prefs.saveProfile(
                name = prefs.getProfileName(),
                email = email.trim(),
                phone = prefs.getProfilePhone()
            )
            LoginUiState.Success
        } else {
            LoginUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    private fun loginWithGoogle(
        idToken: String,
        displayName: String?,
        googleEmail: String?
    ) = viewModelScope.launch {
        _uiState.value = LoginUiState.Loading

        val result = googleLoginUseCase(idToken)
        _uiState.value = if (result.isSuccess) {
            markLoggedIn()
            val resolvedName = displayName?.trim().orEmpty()
                .ifBlank { prefs.getProfileName() }
            val resolvedEmail = googleEmail?.trim().orEmpty()
                .ifBlank { prefs.getProfileEmail() }
            prefs.saveProfile(
                name = resolvedName,
                email = resolvedEmail,
                phone = prefs.getProfilePhone()
            )
            LoginUiState.Success
        } else {
            LoginUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    private fun loginWithFacebook(accessToken: String) = viewModelScope.launch {
        _uiState.value = LoginUiState.Loading

        val result = facebookLoginUseCase(accessToken)
        _uiState.value = if (result.isSuccess) {
            markLoggedIn()
            LoginUiState.Success
        } else {
            LoginUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    fun consumeTransientState() {
        if (_uiState.value is LoginUiState.Success || _uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    private fun markLoggedIn() {
        prefs.saveIsLoggedIn(true)
    }
}
