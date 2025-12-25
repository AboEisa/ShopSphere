package com.example.shopsphere.CleanArchitecture.ui.views

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SplashViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        navigate()
    }

    private fun navigate() {
        lifecycleScope.launch {
            delay(1500)
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            // CASE 1 — User logged in via Firebase
            if (currentUser != null) {

                currentUser.getIdToken(true).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // ✅ FIXED: Sync Firebase auth with SharedPreferences
                        viewModel.syncAuthState(currentUser.uid, isLoggedIn = true)
                        openMain(home = true)
                    } else {
                        // If token fails — clear the session + send to login
                        auth.signOut()
                        // ✅ FIXED: Clear SharedPreferences too
                        viewModel.syncAuthState("", isLoggedIn = false)
                        openMain(home = false)
                    }
                }

            } else {
                // CASE 2 — No Firebase user, check SharedPreferences as fallback
                val isLoggedIn = viewModel.isLoggedIn.first()
                if (isLoggedIn) {
                    // User has valid session in SharedPreferences
                    openMain(home = true)
                } else {
                    // No valid session anywhere
                    openMain(home = false)
                }
            }
        }
    }

    private fun openMain(home: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_OPEN_HOME, home)
        startActivity(intent)
        finish()
    }
}
