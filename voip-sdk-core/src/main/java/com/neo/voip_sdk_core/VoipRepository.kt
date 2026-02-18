package com.neo.voip_sdk_core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class VoipRepository(
    private val engine: SipEngine
) {

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    private val _registrationState =
        MutableStateFlow<RegistrationState>(RegistrationState.None)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    private val _incomingCall =
        MutableSharedFlow<String>()
    val incomingCall: SharedFlow<String> = _incomingCall

    init {
        engine.setListener(object : SipEngineListener {

            override fun onRegistration(state: RegistrationState) {
                _registrationState.value = state
            }

            override fun onCallState(state: CallState) {
                _callState.value = state
            }

            override fun onIncomingCall(from: String) {
                CoroutineScope(Dispatchers.IO).launch {
                    _incomingCall.emit(from)
                }
            }
        })
    }

    fun initialize() = engine.initialize()

    fun login(u: String, p: String, d: String) =
        engine.login(u, p, d)

    fun logout() = engine.logout()

    fun startCall(dest: String) =
        engine.startCall(dest)

    fun acceptCall() = engine.acceptCall()

    fun rejectCall() = engine.rejectCall()

    fun endCall() = engine.endCall()

    fun toggleMute() = engine.toggleMute()

    fun toggleSpeaker() = engine.toggleSpeaker()

    fun destroy() = engine.destroy()
}
