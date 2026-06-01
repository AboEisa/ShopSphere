package com.example.shopsphere.CleanArchitecture.ui.views

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var sharedPreference: SharedPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPersistedLanguage()
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        navigate()
    }

    private fun applyPersistedLanguage() {
        val tag = sharedPreference.getLanguage()
        if (tag.isNotBlank()) {
            val current = AppCompatDelegate.getApplicationLocales()
            if (current.isEmpty || current.toLanguageTags() != tag) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tag)
                )
            }
        }
    }

    private fun navigate() {
        lifecycleScope.launch {
            delay(1500)
            val isLoggedIn = viewModel.isLoggedIn.first()
            openMain(home = isLoggedIn)
        }
    }

    private fun openMain(home: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_OPEN_HOME, home)
        startActivity(intent)
        finish()
    }
}
