package com.example.shopsphere.CleanArchitecture.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefs: SharedPreference
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        val uid = prefs.getUid()
        _isLoggedIn.value = uid.isNotEmpty()
    }
}
