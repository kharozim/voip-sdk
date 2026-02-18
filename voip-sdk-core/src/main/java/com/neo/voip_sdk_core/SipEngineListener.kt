package com.neo.voip_sdk_core

interface SipEngineListener {

    fun onRegistration(state: RegistrationState)

    fun onCallState(state: CallState)

    fun onIncomingCall(from: String)
}
