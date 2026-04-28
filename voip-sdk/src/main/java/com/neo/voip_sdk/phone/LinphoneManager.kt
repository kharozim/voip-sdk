package com.neo.voip_sdk.phone

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.neo.voip_sdk.CallState
import com.neo.voip_sdk.RegistrationState
import com.neo.voip_sdk.SipEngine
import com.neo.voip_sdk.SipEngineListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
  companion object {
    private const val EARPIECE_OUTPUT = 2
    private const val SPEAKER_OUTPUT = 3
    private const val AUDIO_ROUTE_RETRY_COUNT = 6
    private const val AUDIO_ROUTE_RETRY_DELAY_MS = 250L
  }

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private var focusRequest: AudioFocusRequest? = null

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
    Log.e("TAG", "cobacall : initialize")
    factory.setDebugMode(true, "LinphoneManager")

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

  override fun register(username: String, password: String, domain: String) {

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
    Log.e("TAG", "cobacall : register")

  }

  override fun logout() {
    core.accountList.forEach {
      core.removeAccount(it)
    }

    core.clearAllAuthInfo()

    currentCall?.terminate()
    currentCall = null
  }

  private fun requestAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val playbackAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

      focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(playbackAttributes)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener { }
        .build()

      focusRequest?.let { audioManager.requestAudioFocus(it) }
    } else {
      @Suppress("DEPRECATION")
      audioManager.requestAudioFocus(
        { },
        AudioManager.STREAM_VOICE_CALL,
        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
      )
    }
  }

  private fun abandonAudioFocus() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    } else {
      @Suppress("DEPRECATION")
      audioManager.abandonAudioFocus { }
    }
  }

  private fun startAudioComm() {
    requestAudioFocus()
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    Log.d("TAG", "cobacall : startAudioComm mode=${audioManager.mode} speaker=${audioManager.isSpeakerphoneOn}")
  }

  private fun stopAudioComm() {
    audioManager.mode = AudioManager.MODE_NORMAL
    audioManager.isSpeakerphoneOn = false
    abandonAudioFocus()
    Log.d("TAG", "cobacall : stopAudioComm mode reset to NORMAL")
  }

  override fun startCall(destination: String, phoneId: String?) {
    if (currentCall != null) return

    startAudioComm()
    applyPreferredAudioRoute(EARPIECE_OUTPUT)

    val address = factory.createAddress(destination)
    address?.let { address ->
      val params = core.createCallParams(null)
      params ?: return

      params.mediaEncryption = MediaEncryption.None

      phoneId?.let {
        params.addCustomHeader("X-Telphone_ID", it)
      }

      currentCall = core.inviteAddressWithParams(address, params)
    }
  }

  override fun acceptCall() {
    startAudioComm()
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

  override fun toggleSpeaker(output: Int) {
    audioManager.isSpeakerphoneOn = (output == SPEAKER_OUTPUT)
    applyPreferredAudioRoute(output)
  }

  override fun getSpeakerOutput(): List<Int> {
    return core.audioDevices.map { it.type.toInt() }
  }

  override fun destroy() {
    if (!::core.isInitialized) return

    try {
      stopAudioComm()
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

  private fun applyPreferredAudioRoute(output : Int) {
    val audioDevice = core.audioDevices.firstOrNull {
      it.type.toInt() == output
    } ?: core.audioDevices.firstOrNull {
      it.type == AudioDevice.Type.Earpiece
    }

    if (audioDevice == null) {
      Log.w("TAG", "cobacall : no audio device found for output=$output")
      return
    }

    core.outputAudioDevice = audioDevice
    Log.d("TAG", "cobacall : output audio device=${audioDevice.type.name}")
  }

  private val coreListener =
    object : CoreListenerStub() {

      override fun onRegistrationStateChanged(
        core: Core,
        cfg: ProxyConfig,
        state: org.linphone.core.RegistrationState,
        message: String,
      ) {
        Log.e("TAG", "cobacall : onRegistrationStateChanged ${state.name} $message")

        when (state) {
          org.linphone.core.RegistrationState.Ok ->
            listener?.onRegistration(RegistrationState.Registered)

          org.linphone.core.RegistrationState.Failed ->
            listener?.onRegistration(RegistrationState.Failed(message))

          org.linphone.core.RegistrationState.None ->
            listener?.onRegistration(RegistrationState.None)

          org.linphone.core.RegistrationState.Refreshing ->
            listener?.onRegistration(RegistrationState.Refreshing)

          org.linphone.core.RegistrationState.Cleared ->
            listener?.onRegistration(RegistrationState.Cleared)

          org.linphone.core.RegistrationState.Progress ->
            listener?.onRegistration(RegistrationState.Progress)

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
            run {
              startAudioComm()
              listener?.onCallState(CallState.Calling)
            }

          Call.State.OutgoingProgress -> {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            listener?.onCallState(CallState.Ringing)
          }

          Call.State.OutgoingRinging -> {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            listener?.onCallState(CallState.Ringing)
          }

          Call.State.IncomingReceived -> {
            // Kita tidak memanggil startAudioComm di sini agar tidak mengambil focus sebelum user angkat
            listener?.onIncomingCall(
              call.remoteAddress.asStringUriOnly()
            )
          }

          Call.State.StreamsRunning -> {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            listener?.onCallState(CallState.Active)
          }

          Call.State.End,
          Call.State.Released,
            -> {
            stopAudioComm()
            currentCall = null
            listener?.onCallState(CallState.Disconnected)
          }

          Call.State.Error -> {
            stopAudioComm()
            currentCall = null
            listener?.onCallState(CallState.Error(message))
          }

          else -> {}
        }
      }

    }
}
