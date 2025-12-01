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
        checkLoginState()
    }

    // ✅ FIXED: Check BOTH UID and login flag
    private fun checkLoginState() {
        val uid = prefs.getUid()
        val isLoggedIn = prefs.isLoggedIn()

        // User is logged in if BOTH conditions are true
        _isLoggedIn.value = uid.isNotEmpty() && isLoggedIn
    }

    // ✅ NEW: Sync Firebase auth state with SharedPreferences
    fun syncAuthState(uid: String, isLoggedIn: Boolean) {
        if (isLoggedIn && uid.isNotEmpty()) {
            prefs.saveUid(uid)
            prefs.saveIsLoggedIn(true)
        } else {
            prefs.clearUid()
            prefs.saveIsLoggedIn(false)
        }
        _isLoggedIn.value = isLoggedIn
    }
}