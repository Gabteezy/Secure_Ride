package com.aces.capstone.secureride.model

import android.app.Application
import com.google.firebase.FirebaseApp

class SecureRide : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}