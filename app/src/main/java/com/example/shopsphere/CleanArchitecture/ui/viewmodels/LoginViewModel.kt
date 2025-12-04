package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.auth.GoogleLoginUseCase
import com.example.shopsphere.CleanArchitecture.domain.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase,
    private val prefs: SharedPreference
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state

    // ----------------------------------------------------------
    // EMAIL LOGIN
    // ----------------------------------------------------------
    // In LoginViewModel - after successful Firebase login
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val firebaseUser = loginUseCase(email, password)

                // ✅ Firebase is already persisted, just sync SharedPreferences
                prefs.saveUid(firebaseUser.uid)
                prefs.saveIsLoggedIn(true)

                _state.value = AuthUiState.Success("Login success")
            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message)
            }
        }
    }

    // ----------------------------------------------------------
    // GOOGLE LOGIN
    // ----------------------------------------------------------
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading

            try {
                val firebaseUser = googleLoginUseCase(idToken)

                // FIXED: Save both UID and login state
                prefs.saveUid(firebaseUser.uid)
                prefs.saveIsLoggedIn(true)  // ← ADD THIS

                _state.value = AuthUiState.Success("Google login success")

            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message)
            }
        }
    }



    // ----------------------------------------------------------
    // CHECK IF LOGGED IN
    // ----------------------------------------------------------
    fun checkIfLoggedIn() {
        // FIXED: Check explicit login state instead of just UID
        val isLoggedIn = prefs.isLoggedIn()

        if (isLoggedIn) {
            _state.value = AuthUiState.Success("Already logged in")
        } else {
            _state.value = AuthUiState.Idle
        }
    }

    // ----------------------------------------------------------
    // LOGOUT
    // ----------------------------------------------------------
    fun logout() {
        prefs.clearUid()
        prefs.saveIsLoggedIn(false)
        _state.value = AuthUiState.Idle
    }
}