package com.neo.voip_sdk.phone

import com.neo.voip_sdk.CallState
import org.linphone.core.RegistrationState

internal interface LinphoneEventListener {

    fun onRegistration(state: RegistrationState)

    fun onCallState(state: CallState)

    fun onIncomingCall(from: String)
}
