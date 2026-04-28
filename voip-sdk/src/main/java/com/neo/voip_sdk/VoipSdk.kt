package com.neo.voip_sdk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

object VoipSdk {

    private lateinit var repository: VoipRepository
    private var sipEngine: SipEngine? = null
    private var phoneId : String? = null

    fun initialize(engine: SipEngine, phoneId : String? = null) {
        repository = VoipRepository(engine)
        repository.initialize()
        sipEngine = engine
        this.phoneId = phoneId
    }

    fun register(username: String, password: String, domain: String) =
        repository.register(username, password, domain)

    fun logout() = repository.logout()

    fun startCall(destination: String, phoneId: String? = null) =
        repository.startCall(destination, phoneId)

    fun acceptCall() = repository.acceptCall()

    fun rejectCall() = repository.rejectCall()

    fun endCall() = repository.endCall()

    fun toggleMute() = repository.toggleMute()

    fun toggleSpeaker(output : Int) = repository.toggleSpeaker(output)

    fun getSpeakerOutput() : List<Int> = repository.getSpeakerOutput()

    fun observeCallState(): StateFlow<CallState> =
        repository.callState

    fun observeRegistrationState(): StateFlow<RegistrationState> =
        repository.registrationState

    fun observeIncomingCall(): Flow<String> =
        repository.incomingCall

    fun getCallLog(): List<String> = sipEngine?.getCallLog() ?: emptyList()

    fun destroy() = repository.destroy()
}
