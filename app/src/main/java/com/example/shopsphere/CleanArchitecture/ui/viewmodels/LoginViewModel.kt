package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.domain.auth.GoogleLoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase
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

            is LoginUiEvent.GoogleToken -> loginWithGoogle(event.idToken)
        }
    }

    private fun login() = viewModelScope.launch {
        _uiState.value = LoginUiState.Loading

        val result = loginUseCase(email, password)
        _uiState.value =
            if (result.isSuccess) LoginUiState.Success
            else LoginUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
    }

    private fun loginWithGoogle(idToken: String) = viewModelScope.launch {
        _uiState.value = LoginUiState.Loading

        val result = googleLoginUseCase(idToken)
        _uiState.value =
            if (result.isSuccess) LoginUiState.Success
            else LoginUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
    }

    fun consumeTransientState() {
        if (_uiState.value is LoginUiState.Success || _uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
