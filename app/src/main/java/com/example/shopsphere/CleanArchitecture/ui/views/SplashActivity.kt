package com.example.shopsphere.CleanArchitecture.ui.views

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.shopsphere.R
import com.google.firebase.auth.FirebaseAuth
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        navigate()
    }

    private fun navigate() {
        Handler(Looper.getMainLooper()).postDelayed({

            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            // CASE 1 — User logged in via Firebase
            if (currentUser != null) {

                currentUser.getIdToken(true).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        openMain(home = true)
                    } else {
                        // If token fails — clear the session + send to login
                        auth.signOut()
                        openMain(home = false)
                    }
                }

            } else {
                // CASE 2 — No Firebase user found → Send to login
                openMain(home = false)
            }

        }, 1500)
    }

    private fun openMain(home: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("openHome", home)
        startActivity(intent)
        finish()
    }
}
