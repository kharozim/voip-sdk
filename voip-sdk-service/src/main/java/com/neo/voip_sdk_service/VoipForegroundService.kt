package com.neo.voip_sdk_service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.neo.voip_sdk_core.VoipSdk

class VoipForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(
            1001,
            buildNotification()
        )
    }

    private fun buildNotification(): Notification {

        val channelId = "voip_channel"

        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "VoIP Service",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("VoIP Active")
            .setSmallIcon(android.R.drawable.sym_call_outgoing)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        VoipSdk.toggleMute()
    }
}
