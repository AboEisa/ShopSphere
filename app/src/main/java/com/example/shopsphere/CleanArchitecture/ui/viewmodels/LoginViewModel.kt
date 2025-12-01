package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.auth.FacebookLoginUseCase
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
    private val facebookLoginUseCase: FacebookLoginUseCase,
    private val prefs: SharedPreference
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state

    // ----------------------------------------------------------
    // EMAIL LOGIN
    // ----------------------------------------------------------
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading

            try {
                val firebaseUser = loginUseCase(email, password)   // returns FirebaseUser
                prefs.saveUid(firebaseUser.uid)

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
                val firebaseUser = googleLoginUseCase(idToken)   // returns FirebaseUser
                prefs.saveUid(firebaseUser.uid)

                _state.value = AuthUiState.Success("Google login success")

            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message)
            }
        }
    }

    // ----------------------------------------------------------
    // FACEBOOK LOGIN
    // ----------------------------------------------------------
    fun loginWithFacebook(accessToken: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading

            try {
                val result = facebookLoginUseCase(accessToken)  // returns Result<FirebaseUser>

                val firebaseUser = result.getOrThrow()
                prefs.saveUid(firebaseUser.uid)

                _state.value = AuthUiState.Success("Facebook login success")

            } catch (e: Exception) {
                _state.value = AuthUiState.Error(e.message)
            }
        }
    }

    // ----------------------------------------------------------
    // CHECK IF LOGGED IN
    // ----------------------------------------------------------
    fun checkIfLoggedIn() {
        val uid = prefs.getUid()

        if (uid.isNotEmpty()) {
            _state.value = AuthUiState.Success("Already logged in")
        }
    }
}
