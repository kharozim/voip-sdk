package com.neo.voip_sdk.phone

import android.content.Context
import android.util.Log
import com.neo.voip_sdk.CallState
import com.neo.voip_sdk.RegistrationState
import com.neo.voip_sdk.SipEngine
import com.neo.voip_sdk.SipEngineListener
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.ProxyConfig
import org.linphone.core.Reason
import org.linphone.core.TransportType

internal class LinphoneManager(
  private val context: Context,
) : SipEngine {

  private val factory = Factory.instance()
  private lateinit var core: Core
  private var currentCall: Call? = null
  private var listener: SipEngineListener? = null

  override fun setListener(listener: SipEngineListener) {
    this.listener = listener
  }

  override fun getCallLog(): List<String> {
    return core.callLogs.map { log ->
      val direction = if (log.dir == Call.Dir.Incoming) "Incoming" else "Outgoing"
      val from = log.fromAddress.asStringUriOnly()
      val to = log.toAddress.asStringUriOnly()
      val status = log.status.name
      "$direction | From: $from | To: $to | Status: $status"
    }
  }

  private var isInitialized = false
  override fun initialize() {

    if (isInitialized) return
    Log.e("TAG", "cobacall : init ialize")
    factory.setDebugMode(true, "cobacall")

    core = factory.createCore(null, null, context)
    // Echo cancellation
    core.isEchoCancellationEnabled = true

    // Optional: enable adaptive jitter buffer (recommended)
    core.isAdaptiveRateControlEnabled = true

    // Start core (Linphone 5.x auto iterate)
    core.start()

    core.addListener(coreListener)

    isInitialized = true
  }

  override fun login(username: String, password: String, domain: String) {

    val authInfo = factory.createAuthInfo(
      username,
      null,
      password,
      null,
      null,
      domain
    )

    val params = core.createAccountParams()
    val identityAddress = factory.createAddress("sip:$username@$domain")
    params.identityAddress = identityAddress

    val serverAddress = factory.createAddress("sip:$domain")
    serverAddress?.transport = TransportType.Udp
    params.serverAddress = serverAddress

    params.isRegisterEnabled = true

    val account = core.createAccount(params)

    core.addAuthInfo(authInfo)
    core.addAccount(account)
    core.defaultAccount = account
    Log.e("TAG", "cobacall : login")

  }

  override fun logout() {

    core.accountList.forEach {
      core.removeAccount(it)
    }

    core.clearAllAuthInfo()

    currentCall?.terminate()
    currentCall = null
  }

  override fun startCall(destination: String) {
    val address = factory.createAddress(destination)
    address?.let { address ->
      // We also need a CallParams object
      // Create call params expects a Call object for incoming calls, but for outgoing we must use null safely
      val params = core.createCallParams(null)
      params ?: return // Same for params

      // We can now configure it
      // Here we ask for no encryption but we could ask for ZRTP/SRTP/DTLS
      params.mediaEncryption = MediaEncryption.None
      // If we wanted to start the call with video directly
      //params.enableVideo(true)

      // Finally we start the call
      // Call process can be followed in onCallStateChanged callback from core listener
      currentCall = core.inviteAddressWithParams(address, params)
    }
  }

  override fun acceptCall() {
    currentCall?.accept()
  }

  override fun rejectCall() {
    currentCall?.decline(Reason.Declined)
  }

  override fun endCall() {
    currentCall?.terminate()
    currentCall = null
  }

  override fun toggleMute() {
    currentCall?.let {
      it.microphoneMuted = !it.microphoneMuted
    }
  }

  override fun toggleSpeaker() {
    val speaker = core.audioDevices.firstOrNull {
      it.type == AudioDevice.Type.Speaker
    }
    speaker?.let { core.outputAudioDevice = it }
  }

  override fun destroy() {
    if (!::core.isInitialized) return

    try {
      currentCall?.terminate()
      currentCall = null

      core.accountList.forEach {
        core.removeAccount(it)
      }

      core.clearAllAuthInfo()

      core.removeListener(coreListener)

      core.stop()

    } catch (e: Exception) {
      Log.e("Voip", "Destroy error", e)
    }
  }

  private val coreListener =
    object : CoreListenerStub() {

      override fun onRegistrationStateChanged(
        core: Core,
        cfg: ProxyConfig,
        state: org.linphone.core.RegistrationState,
        message: String,
      ) {
        Log.e("TAG", "cobacall : onRegistrationStateChanged ${state.name}")

        when (state) {
          org.linphone.core.RegistrationState.Ok ->
            listener?.onRegistration(RegistrationState.Registered)

          org.linphone.core.RegistrationState.Failed ->
            listener?.onRegistration(RegistrationState.Failed)

          else ->
            listener?.onRegistration(RegistrationState.Registering)
        }
      }

      override fun onCallStateChanged(
        core: Core,
        call: Call,
        state: Call.State,
        message: String,
      ) {

        currentCall = call
        Log.e("TAG", "cobacall : onCallStateChanged ${state.name}")

        when (state) {

          Call.State.OutgoingInit ->
            listener?.onCallState(CallState.Calling)

          Call.State.IncomingReceived ->
            listener?.onIncomingCall(
              call.remoteAddress.asStringUriOnly()
            )

          Call.State.StreamsRunning ->
            listener?.onCallState(CallState.Active)

          Call.State.End ->
            listener?.onCallState(CallState.Disconnected)

          Call.State.Error ->
            listener?.onCallState(CallState.Error(message))

          else -> {}
        }
      }

    }
}
