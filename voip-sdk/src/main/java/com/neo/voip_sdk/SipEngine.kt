package com.neo.voip_sdk

import android.content.Context
import com.neo.voip_sdk.phone.LinphoneManager

interface SipEngine {

    fun initialize()

    fun login(username: String, password: String, domain: String)

    fun logout()

    fun startCall(destination: String)

    fun acceptCall()

    fun rejectCall()

    fun endCall()

    fun toggleMute()

    fun toggleSpeaker()

    fun destroy()

    fun setListener(listener: SipEngineListener)

    fun getCallLog(): List<String>

    companion object {
        fun build(context: Context) : SipEngine = LinphoneManager(context)
    }
}
