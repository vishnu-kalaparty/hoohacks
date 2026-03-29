package com.example.cadence

import android.app.Application
import com.presagetech.smartspectra.SmartSpectraSdk

class AurisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the SDK once here for the entire app life
        SmartSpectraSdk.initialize(this)
    }
}