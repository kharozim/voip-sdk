package com.neo.voip_sdk.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.neo.voip_sdk.VoipSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NeoCallService : Service() {

  private var isForegroundStarted = false

  private var timerJob: Job? = null

  override fun onCreate() {
    super.onCreate()
    startForeground(
      1001,
      buildNotification()
    )
    isForegroundStarted = true
  }

  private fun buildNotification(): Notification {
    val channelId = "voip_channel"

    val manager =
      getSystemService(NOTIFICATION_SERVICE)
        as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.createNotificationChannel(
        NotificationChannel(
          channelId,
          "NeoCall Service",
          NotificationManager.IMPORTANCE_LOW
        )
      )
    }

    return NotificationCompat.Builder(this, channelId)
      .setContentTitle("NeoCall Active")
      .setSmallIcon(R.drawable.sym_call_outgoing)
      .setOngoing(true)
      .build()
  }

  var onTickerUpdate: ((seconds: Long) -> Unit)? = null
  fun startCallTimer() {
    timerJob?.cancel()
    timerJob = CoroutineScope(Dispatchers.Default).launch {
      var seconds = 0L
      while (isActive) {
        delay(1000)
        seconds++
        onTickerUpdate?.invoke(seconds)
      }
    }
  }

  fun stopTimer() {
    timerJob?.cancel()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      Action.OUTGOING -> {}
      Action.ONGOING -> {
        startCallTimer()
      }

      Action.STOP -> {
        stopTimer()
        stopForegroundService()
        stopSelf()
      }
    }

    return START_STICKY
  }

  private fun stopForegroundService() {
    if (!isForegroundStarted) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION")
      stopForeground(true)
    }
    isForegroundStarted = false
  }

  object Action {
    const val OUTGOING = "com.neo.voip_sdk.OUTGOING"
    const val ONGOING = "com.neo.voip_sdk.ONGOING"
    const val STOP = "com.neo.voip_sdk.STOP"
  }

  override fun onDestroy() {
    stopTimer()
    stopForegroundService()
    super.onDestroy()
  }
}
