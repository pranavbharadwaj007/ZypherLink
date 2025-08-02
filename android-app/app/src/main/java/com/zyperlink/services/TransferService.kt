package com.zyperlink.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zyperlink.R
import com.zyperlink.utils.Constants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransferService : Service() {
    
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRANSFER -> {
                startForegroundService()
            }
            ACTION_STOP_TRANSFER -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun startForegroundService() {
        val notification = createTransferNotification()
        startForeground(NOTIFICATION_ID_TRANSFER, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_TRANSFER,
                "File Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of file transfers"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createTransferNotification(): Notification {
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_TRANSFER)
            .setContentTitle("ZypherLink Transfer")
            .setContentText("Transferring files...")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }
    
    fun updateTransferProgress(progress: Int, max: Int, fileName: String) {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_TRANSFER)
            .setContentTitle("Transferring: $fileName")
            .setContentText("$progress / $max files")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setProgress(max, progress, false)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_TRANSFER, notification)
    }
    
    companion object {
        const val ACTION_START_TRANSFER = "com.zyperlink.START_TRANSFER"
        const val ACTION_STOP_TRANSFER = "com.zyperlink.STOP_TRANSFER"
        
        private const val NOTIFICATION_ID_TRANSFER = 1001
    }
}