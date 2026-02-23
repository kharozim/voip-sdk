package com.neo.voip_sdk

import android.Manifest
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

object VoipSdk {

    private lateinit var repository: VoipRepository

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun initialize(engine: SipEngine) {
        repository = VoipRepository(engine)
        repository.initialize()
    }

    fun login(username: String, password: String, domain: String) =
        repository.login(username, password, domain)

    fun logout() = repository.logout()

    fun startCall(destination: String) =
        repository.startCall(destination)

    fun acceptCall() = repository.acceptCall()

    fun rejectCall() = repository.rejectCall()

    fun endCall() = repository.endCall()

    fun toggleMute() = repository.toggleMute()

    fun toggleSpeaker() = repository.toggleSpeaker()

    fun observeCallState(): StateFlow<CallState> =
        repository.callState

    fun observeRegistrationState(): StateFlow<RegistrationState> =
        repository.registrationState

    fun observeIncomingCall(): Flow<String> =
        repository.incomingCall

    fun destroy() = repository.destroy()
}
