package com.example.shopsphere.CleanArchitecture.ui.viewmodels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val registerUseCase: RegisterUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state
    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            val res = registerUseCase(name, email, password)
            if (res.isSuccess) _state.value =
                AuthUiState.Success("Registered")
            else _state.value =
                AuthUiState.Error(res.exceptionOrNull()?.message)
        }
    }
}