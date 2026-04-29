package com.neo.sipapp

import android.app.Application
import com.neo.voip_sdk.CallSdk

/**
 * Created by Kharozim
 * 29/04/26 - kharozim.wrk@gmail.com
 * Copyright (c) 2026. SIPApp
 * All Rights Reserved
 */
class MyApp : Application() {
  override fun onCreate() {
    super.onCreate()
    CallSdk.init(this.applicationContext)
  }
}