package com.neo.voip_sdk

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.neo.voip_sdk.ui.CallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference

/**
 * Created by Kharozim
 * 17/04/26 - kharozim.wrk@gmail.com
 * Copyright (c) 2026. SIPApp
 * All Rights Reserved
 */
object CallSdk {
  private var contextRef: WeakReference<Context>? = null

  fun init(context: Context): CallSdk {
    contextRef = WeakReference(context.applicationContext)
    return this
  }

  private val requiredPermissions = arrayOf(
    android.Manifest.permission.RECORD_AUDIO,
    android.Manifest.permission.READ_PHONE_STATE,
  )

  @RequiresApi(Build.VERSION_CODES.P)
  private val requiredPermissions28 = arrayOf(
    android.Manifest.permission.RECORD_AUDIO,
    android.Manifest.permission.FOREGROUND_SERVICE,
    android.Manifest.permission.READ_PHONE_STATE,
  )

  @RequiresApi(Build.VERSION_CODES.S)
  private val requiredPermissions31 = arrayOf(
    android.Manifest.permission.RECORD_AUDIO,
    android.Manifest.permission.FOREGROUND_SERVICE,
    android.Manifest.permission.READ_PHONE_STATE,
    android.Manifest.permission.BLUETOOTH_CONNECT,
  )

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private val requiredPermissionsTirmaisu = arrayOf(
    android.Manifest.permission.RECORD_AUDIO,
    android.Manifest.permission.FOREGROUND_SERVICE,
    android.Manifest.permission.POST_NOTIFICATIONS,
    android.Manifest.permission.READ_PHONE_STATE,
    android.Manifest.permission.BLUETOOTH_CONNECT,
  )

  @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
  private val requiredPermissionsUpsideDownCake = arrayOf(
    android.Manifest.permission.RECORD_AUDIO,
    android.Manifest.permission.FOREGROUND_SERVICE,
    android.Manifest.permission.POST_NOTIFICATIONS,
    android.Manifest.permission.READ_PHONE_STATE,
    android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
    android.Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE,
    android.Manifest.permission.BLUETOOTH_CONNECT
  )


  @OptIn(ExperimentalCoroutinesApi::class)
  private suspend fun isForegroundMicPermissionGranted(activity: ComponentActivity): Boolean {
    val ctx = contextRef?.get() ?: return false

    val permissions = when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> requiredPermissionsUpsideDownCake
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> requiredPermissionsTirmaisu
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> requiredPermissions31
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> requiredPermissions28
      else -> requiredPermissions
    }

    val notGranted = permissions.filter {
      ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
    }

    if (notGranted.isEmpty()) return true

    return suspendCancellableCoroutine { continuation ->
      // val requestCode = 1001

      val callback = ActivityResultCallback<Map<String, Boolean>> { result ->
        val allGranted = result.values.all { it }
//        continuation.resume(allGranted) { cause, _, _ -> }
        continuation.resumeWith(Result.success(allGranted))
      }

      val launcher = activity.activityResultRegistry.register(
        "permission_request_${System.currentTimeMillis()}",
        ActivityResultContracts.RequestMultiplePermissions(),
        callback
      )

      launcher.launch(notGranted.toTypedArray())
    }
  }


  fun makeCall(
    activity: ComponentActivity,
    callerId: String,
    callerName: String? = "Caller",
    callerAvatar: String? = "",
    calleeId: String,
    calleeName: String? = "Callee",
    calleeAvatar: String = "",
    checkSum: String,
    username: String,
    password: String,
    domain: String,
    destination: String,
    metaData: Map<String, String> = emptyMap(),
  ) {
    val ctx = contextRef?.get() ?: return
    CoroutineScope(Dispatchers.Main).launch {
      if (isForegroundMicPermissionGranted(activity)) {
        val caller = if (callerName.isNullOrBlank()) "Caller" else callerName
        val callee = if (calleeName.isNullOrBlank()) "Callee" else calleeName

        val intent = Intent(ctx, CallActivity::class.java).apply {
//          action = NeoCallService.ACTION.OUTGOING
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
          putExtra("call_type", "outgoing")
          putExtra("callee_id", calleeId)
          putExtra("callee_name", callee)
          putExtra("callee_avatar", calleeAvatar)
          putExtra("caller_id", callerId)
          putExtra("caller_name", caller)
          putExtra("caller_avatar", callerAvatar)
          putExtra("checksum", checkSum)
          putExtra("username", username)
          putExtra("password", password)
          putExtra("domain", domain)
          putExtra("destination", destination)
          putExtra("meta_data", HashMap(metaData))
        }
        ctx.startActivity(intent)
      } else {
//        MessageListenerHolder.callEventListener?.onError(101, "Permisssion not granted")
      }
    }
  }
}
