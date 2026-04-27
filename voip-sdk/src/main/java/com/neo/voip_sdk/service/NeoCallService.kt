package com.neo.voip_sdk.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NeoCallService : Service() {
  companion object {
    private const val NOTIFICATION_ID = 1001
    private const val CHANNEL_ID = "voip_channel"
  }

  private var isForegroundStarted = false
  private val binder = LocalBinder()
  private var timerJob: Job? = null
  private var callStartTimeMillis = 0L
  private var currentDurationSeconds = 0L
  private var lastCallDurationSeconds = 0L
  private var onTickerUpdate: ((seconds: Long) -> Unit)? = null

  inner class LocalBinder : Binder() {
    fun getService(): NeoCallService = this@NeoCallService
  }

  override fun onCreate() {
    super.onCreate()
    startForeground(
      NOTIFICATION_ID,
      buildNotification()
    )
    isForegroundStarted = true
  }

  private fun buildNotification(durationSeconds: Long = currentDurationSeconds): Notification {
    val manager =
      getSystemService(NOTIFICATION_SERVICE)
        as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      manager.createNotificationChannel(
        NotificationChannel(
          CHANNEL_ID,
          "NeoCall Service",
          NotificationManager.IMPORTANCE_LOW
        )
      )
    }

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("Call Active")
      .setContentText("Durasi ${formatElapsedTime(durationSeconds)}")
      .setSmallIcon(R.drawable.sym_call_outgoing)
      .setOngoing(true)
      .build()
  }

  fun setOnTickerUpdateListener(listener: ((seconds: Long) -> Unit)?) {
    onTickerUpdate = listener
    listener?.invoke(currentDurationSeconds)
  }

  fun getCurrentDurationSeconds(): Long = currentDurationSeconds

  fun getLastCallDurationSeconds(): Long = lastCallDurationSeconds

  fun startCallTimer() {
    if (timerJob?.isActive == true) return

    if (callStartTimeMillis == 0L) {
      callStartTimeMillis = System.currentTimeMillis()
      currentDurationSeconds = 0L
      lastCallDurationSeconds = 0L
      publishTicker()
    }

    timerJob = CoroutineScope(Dispatchers.Default).launch {
      while (isActive) {
        delay(1000)
        currentDurationSeconds = (System.currentTimeMillis() - callStartTimeMillis) / 1000
        publishTicker()
      }
    }
  }

  fun stopTimer() {
    timerJob?.cancel()
    timerJob = null
    if (callStartTimeMillis != 0L) {
      currentDurationSeconds = (System.currentTimeMillis() - callStartTimeMillis) / 1000
      lastCallDurationSeconds = currentDurationSeconds
      callStartTimeMillis = 0L
      publishTicker()
    }
  }

  private fun publishTicker() {
    onTickerUpdate?.invoke(currentDurationSeconds)
    updateNotification(currentDurationSeconds)
  }

  private fun updateNotification(durationSeconds: Long) {
    val manager =
      getSystemService(NOTIFICATION_SERVICE)
        as NotificationManager
    manager.notify(NOTIFICATION_ID, buildNotification(durationSeconds))
  }

  override fun onBind(intent: Intent?): IBinder = binder

  override fun onUnbind(intent: Intent?): Boolean {
    onTickerUpdate = null
    return super.onUnbind(intent)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      Action.OUTGOING -> {
        updateNotification(currentDurationSeconds)
      }

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
    timerJob?.cancel()
    timerJob = null
    onTickerUpdate = null
    stopForegroundService()
    super.onDestroy()
  }

  private fun formatElapsedTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
  }
}
