package com.neo.voip_sdk_linphone

import com.neo.voip_sdk_core.CallState
import com.neo.voip_sdk_core.RegistrationState

interface LinphoneEventListener {

    fun onRegistration(state: RegistrationState)

    fun onCallState(state: CallState)

    fun onIncomingCall(from: String)
}
