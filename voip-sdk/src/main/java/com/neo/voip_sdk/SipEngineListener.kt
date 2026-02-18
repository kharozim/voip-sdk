package com.neo.voip_sdk

interface SipEngineListener {

    fun onRegistration(state: RegistrationState)

    fun onCallState(state: CallState)

    fun onIncomingCall(from: String)
}
