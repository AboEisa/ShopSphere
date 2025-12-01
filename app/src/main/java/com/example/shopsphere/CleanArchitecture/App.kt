package com.example.shopsphere.CleanArchitecture

import android.app.Application
import com.stripe.android.PaymentConfiguration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51N4k1bLz2Z5Y3X9Q1c7x2zGQeN7m7AtpFg4YkVKqG61KtvYPRx8yL9fThAtHV9ZoASQkQ7BHOcDnMBo2ULGMaJX300C3bBoJx3"
        )


    }
}