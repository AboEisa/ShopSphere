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

    init {
        Log.d(TAG, "RegisterViewModel initialized")
    }

    fun register(name: String, email: String, password: String) {
        Log.d(TAG, "register() called with name: $name, email: $email")

        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting state to Loading")
                _state.value = AuthUiState.Loading

                Log.d(TAG, "Calling registerUseCase")
                val result = registerUseCase(name, email, password)

                if (result.isSuccess) {
                    val uid = result.getOrNull() ?: ""
                    Log.d(TAG, "Registration successful! UID: $uid")

                    if (uid.isNotEmpty()) {
                        Log.d(TAG, "Saving UID to SharedPreferences")
                        prefs.saveUid(uid)

                        Log.d(TAG, "Saving isLoggedIn = true")
                        prefs.saveIsLoggedIn(true)

                        Log.d(TAG, "Verifying save - UID: ${prefs.getUid()}, isLoggedIn: ${prefs.isLoggedIn()}")
                    } else {
                        Log.w(TAG, "UID is empty!")
                    }

                    Log.d(TAG, "Setting state to Success")
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
}