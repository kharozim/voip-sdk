package com.neo.voip_sdk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object VoipSdk {

    private var repository: VoipRepository? = null
    private var sipEngine: SipEngine? = null

    // optional: expose init status to UI if needed
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    fun initialize(engine: SipEngine) {
        // idempotent: jangan bikin repo baru kalau engine sama
        if (sipEngine === engine && repository != null) return

        // kalau sebelumnya ada repo/engine, rapikan dulu
        runCatching { repository?.destroy() }

        sipEngine = engine
        repository = VoipRepository(engine).also { it.initialize() }
        _isInitialized.value = true
    }

    private fun repo(): VoipRepository {
        return repository ?: error("VoipSdk is not initialized. Call VoipSdk.initialize(engine) first.")
    }

    fun login(username: String, password: String, domain: String) =
        repo().login(username, password, domain)

    fun logout() = repo().logout()

    fun startCall(destination: String) =
        repo().startCall(destination)

    fun acceptCall() = repo().acceptCall()

    fun rejectCall() = repo().rejectCall()

    fun endCall() = repo().endCall()

    fun toggleMute() = repo().toggleMute()

    fun toggleSpeaker() = repo().toggleSpeaker()

    fun observeCallState(): StateFlow<CallState> =
        repo().callState

    fun observeRegistrationState(): StateFlow<RegistrationState> =
        repo().registrationState

    fun observeIncomingCall(): Flow<String> =
        repo().incomingCall

    fun getCallLog(): List<String> = sipEngine?.getCallLog() ?: emptyList()

    fun destroy() {
        runCatching { repository?.destroy() }
        repository = null
        sipEngine = null
        _isInitialized.value = false
    }
}