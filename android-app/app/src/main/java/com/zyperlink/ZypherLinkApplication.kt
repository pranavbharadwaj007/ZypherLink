package com.zyperlink

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ZypherLinkApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global configurations here
    }
}