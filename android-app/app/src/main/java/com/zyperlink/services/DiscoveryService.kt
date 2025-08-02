package com.zyperlink.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.zyperlink.network.DiscoveryManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DiscoveryService : Service() {
    
    @Inject
    lateinit var discoveryManager: DiscoveryManager
    
    private val TAG = "DiscoveryService"
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Discovery service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DISCOVERY -> {
                Log.d(TAG, "Starting discovery service")
                // Start discovery logic here
            }
            ACTION_STOP_DISCOVERY -> {
                Log.d(TAG, "Stopping discovery service")
                discoveryManager.stopDiscovery()
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        discoveryManager.stopDiscovery()
        Log.d(TAG, "Discovery service destroyed")
    }
    
    companion object {
        const val ACTION_START_DISCOVERY = "com.zyperlink.START_DISCOVERY"
        const val ACTION_STOP_DISCOVERY = "com.zyperlink.STOP_DISCOVERY"
    }
}