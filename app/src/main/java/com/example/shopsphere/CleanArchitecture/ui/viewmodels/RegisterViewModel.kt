package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val msg: String = "") : AuthUiState()
    data class Error(val error: String?) : AuthUiState()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val prefs: SharedPreference
) : ViewModel() {

    companion object {
        private const val TAG = "RegisterViewModel"
    }

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state

    fun register(firstName: String, lastName: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                _state.value = AuthUiState.Loading

                val result = registerUseCase(firstName, lastName, email, password)

                if (result.isSuccess) {
                    val uid = result.getOrNull().orEmpty()
                    markLoggedIn(uid)
                    val fullName = "$firstName $lastName".trim()
                    prefs.saveProfile(
                        name = fullName,
                        email = email.trim(),
                        phone = prefs.getProfilePhone()
                    )
                    _state.value = AuthUiState.Success("Account created successfully!")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Registration failed"
                    Log.e(TAG, "Registration failed: $error")
                    _state.value = AuthUiState.Error(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during registration: ${e.message}", e)
                _state.value = AuthUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun consumeTransientState() {
        if (_state.value is AuthUiState.Success || _state.value is AuthUiState.Error) {
            _state.value = AuthUiState.Idle
        }
    }

    private fun markLoggedIn(uid: String) {
        if (uid.isNotBlank()) {
            prefs.saveUid(uid)
            prefs.saveIsLoggedIn(true)
            Log.d(TAG, "Session saved. UID: $uid")
        } else {
            Log.w(TAG, "UID is empty, session was not persisted")
        }
    }
}
