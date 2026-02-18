package com.neo.voip_sdk

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
}
