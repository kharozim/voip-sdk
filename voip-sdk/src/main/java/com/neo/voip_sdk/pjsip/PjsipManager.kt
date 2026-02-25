package com.neo.voip_sdk

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.neo.voip_sdk.pjsip.MyAccount
import com.neo.voip_sdk.pjsip.MyCall
import kotlinx.coroutines.*
import kotlinx.coroutines.asCoroutineDispatcher
import org.pjsip.pjsua2.*
import java.util.concurrent.Executors

class PjsipManager(context: Context) : SipEngine {

  private val appContext = context.applicationContext

  // serialize all PJSUA2 operations
  private val executor = Executors.newSingleThreadExecutor()
  private val dispatcher = executor.asCoroutineDispatcher()
  private val scope = CoroutineScope(SupervisorJob() + dispatcher)
  private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private var listener: SipEngineListener? = null

  private val audioManager by lazy {
    appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }

  // PJSUA2 core
  private var ep: Endpoint? = null
  private var transportId: Int = -1
  private var account: MyAccount? = null
  private var currentCall: MyCall? = null

  private var initialized = false
  private var muted = false
  private var speakerOn = false

  // ---------------- SipEngine ----------------

  override fun setListener(listener: SipEngineListener) {
    this.listener = listener
  }

  override fun getCallLog(): List<String> = emptyList()

  override fun initialize() {
    scope.launch {
      runCatching {
        initInternalIfNeeded()
      }
        .onFailure { postCall(CallState.Error(it.message ?: "INIT_FAILED")) }
    }
  }

  override fun login(username: String, password: String, domain: String) {
    scope.launch {
      try {
        initInternalIfNeeded()
        val endpoint = ep ?: throw IllegalStateException("Endpoint null")

        postReg(RegistrationState.Registering)

        // cleanup old session
        currentCall?.safeHangup()
        currentCall = null
        account?.delete()
        account = null

        val host = domain.trim()

        val accCfg = AccountConfig().apply {
          idUri = "sip:$username@$host"
          regConfig.registrarUri = "sip:$host"

          sipConfig.authCreds.clear()
          sipConfig.authCreds.add(AuthCredInfo("digest", "*", username, 0, password))

          // NAT optional (enable if Asterisk is outside LAN)
          // natConfig.stunServer.clear()
          // natConfig.stunServer.add("stun:stun.l.google.com:19302")
        }

        val myAcc = MyAccount(
          endpoint = endpoint,
          callbacks = object : MyAccount.Callbacks {
            override fun onRegistration(active: Boolean, code: Int, reason: String) {
              // PJSIP sometimes reports active=false on intermediate states.
              // We treat active=true as Registered, others as Failed.
              if (active) postReg(RegistrationState.Registered)
              else postReg(RegistrationState.Failed)
            }

            override fun onIncoming(call: MyCall) {
              // single-call policy
              scope.launch {
                currentCall?.safeHangup()
                currentCall = call

                val from = call.remoteUriSafe()
                postIncoming(from)
                postCall(CallState.Ringing) // incoming call -> UI can show ringing
              }
            }
          }
        )

        myAcc.create(accCfg)
        account = myAcc
      } catch (t: Throwable) {
        postReg(RegistrationState.Failed)
      }
    }
  }

  override fun logout() {
    scope.launch {
      runCatching { currentCall?.safeHangup() }
      currentCall = null
      runCatching { account?.delete() }
      account = null
      postReg(RegistrationState.None)
    }
  }

  override fun startCall(destination: String) {
    scope.launch {
      try {
        val endpoint = ep ?: throw IllegalStateException("Not initialized")
        val acc = account ?: throw IllegalStateException("Not logged in")

        val destUri = normalizeDestination(destination, acc.getDomainFromIdUri())

        currentCall?.safeHangup()
        currentCall = null

        postCall(CallState.Calling)

        val call = MyCall(
          acc = acc,
          endpoint = endpoint,
          callbacks = object : MyCall.Callbacks {
            override fun onCallState(state: Int, code: Int, reason: String) {
              // Map PJSIP INV state (int constants) -> your CallState
              when (state) {
                pjsip_inv_state.PJSIP_INV_STATE_CALLING -> postCall(CallState.Calling)

                // EARLY often means ringing/progress
                pjsip_inv_state.PJSIP_INV_STATE_EARLY -> postCall(CallState.Ringing)

                pjsip_inv_state.PJSIP_INV_STATE_CONNECTING -> postCall(CallState.Connected)

                pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> postCall(CallState.Active)

                pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED -> {
                  postCall(CallState.Disconnected)
                  scope.launch { currentCall = null }
                }

                // incoming handled by account callback
                pjsip_inv_state.PJSIP_INV_STATE_INCOMING -> postCall(CallState.Ringing)

                else -> {
                  // ignore
                }
              }
            }

            override fun onMediaState(active: Boolean) {
              // When media active -> audio connected.
              if (active) {
                applySpeakerInternal(speakerOn)
                applyMuteInternal(muted)
                // Some apps treat "media active" as Active state.
                postCall(CallState.Active)
              }
            }
          }
        )

        val prm = CallOpParam(true).apply {
          opt.videoCount = 0
          opt.audioCount = 1
        }

        call.makeCall(destUri, prm)
        currentCall = call
      } catch (t: Throwable) {
        postCall(CallState.Error(t.message ?: "CALL_FAILED"))
      }
    }
  }

  override fun acceptCall() {
    scope.launch {
      val call = currentCall ?: return@launch
      runCatching {
        val prm = CallOpParam(true).apply { statusCode = pjsip_status_code.PJSIP_SC_OK }
        call.answer(prm)
        postCall(CallState.Connected)
      }.onFailure {
        postCall(CallState.Error(it.message ?: "ACCEPT_FAILED"))
      }
    }
  }

  override fun rejectCall() {
    scope.launch {
      val call = currentCall ?: return@launch
      runCatching {
        val prm = CallOpParam(true).apply { statusCode = pjsip_status_code.PJSIP_SC_DECLINE }
        call.hangup(prm)
        currentCall = null
        postCall(CallState.Disconnected)
      }.onFailure {
        postCall(CallState.Error(it.message ?: "REJECT_FAILED"))
      }
    }
  }

  override fun endCall() {
    scope.launch {
      val call = currentCall ?: return@launch
      runCatching {
        call.safeHangup()
        currentCall = null
        postCall(CallState.Disconnected)
      }.onFailure {
        postCall(CallState.Error(it.message ?: "END_FAILED"))
      }
    }
  }

  override fun toggleMute() {
    scope.launch {
      muted = !muted
      applyMuteInternal(muted)
    }
  }

  override fun toggleSpeaker() {
    scope.launch {
      speakerOn = !speakerOn
      applySpeakerInternal(speakerOn)
    }
  }

  override fun destroy() {
    scope.launch {
      runCatching { currentCall?.safeHangup() }
      currentCall = null

      runCatching { account?.delete() }
      account = null

      runCatching {
        ep?.let { endpoint ->
          runCatching { endpoint.libDestroy() }
          runCatching { endpoint.delete() }
        }
      }
      ep = null
      initialized = false

      scope.cancel()
      mainScope.cancel()
      executor.shutdown()
    }
  }

  // ---------------- internal ----------------
  private fun initInternalIfNeeded() {
    if (initialized) return

    // Load native libs (order can matter)
    runCatching { System.loadLibrary("c++_shared") }
    System.loadLibrary("pjsua2")

    val endpoint = Endpoint()
    endpoint.libCreate()

    val epConfig = EpConfig().apply {
      logConfig.level = 4
      logConfig.consoleLevel = 4

      // audio only config (safe default)
      medConfig.sndClockRate = 16000
      medConfig.ecOptions = 1
      medConfig.noVad = false
    }

    endpoint.libInit(epConfig)

    val tcfg = TransportConfig().apply { port = 5060 }
    transportId = endpoint.transportCreate(
      pjsip_transport_type_e.PJSIP_TRANSPORT_UDP,
      tcfg
    )

    endpoint.libStart()

    ep = endpoint
    initialized = true

    // Notifier: we don't have onEngineReady in listener,
    // so we leave it silent (or set CallState.Idle)
    postCall(CallState.Idle)
    postReg(RegistrationState.None)
  }

  private fun postReg(state: RegistrationState) {
    mainScope.launch { listener?.onRegistration(state) }
  }

  private fun postCall(state: CallState) {
    mainScope.launch { listener?.onCallState(state) }
  }

  private fun postIncoming(from: String) {
    mainScope.launch { listener?.onIncomingCall(from) }
  }

  private fun normalizeDestination(dest: String, domain: String?): String {
    val d = dest.trim()
    return when {
      d.startsWith("sip:") -> d
      d.contains("@") -> "sip:$d"
      !domain.isNullOrBlank() -> "sip:$d@$domain"
      else -> "sip:$d"
    }
  }

  private fun applySpeakerInternal(enabled: Boolean) {
    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    audioManager.isSpeakerphoneOn = enabled
  }

  /**
   * Mute via media routing:
   * - muted: disconnect capture->call and call->playback
   * - unmuted: connect capture->call and call->playback
   */
  private fun applyMuteInternal(enable: Boolean) {
    val endpoint = ep ?: return
    val call = currentCall ?: return

    try {
      val ci = call.info
      for (i in 0 until ci.media.size) {
        val mi = ci.media[i]
        if (mi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
          mi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
        ) {
          val am = call.getAudioMedia(i)
          val adm = endpoint.audDevManager()
          val cap = adm.captureDevMedia
          val play = adm.playbackDevMedia

          cap.stopTransmit(am)
          am.stopTransmit(play)

          if (!enable) {
            cap.startTransmit(am)
            am.startTransmit(play)
          }
          return
        }
      }
    } catch (_: Throwable) {
      // ignore
    }
  }

  companion object {
    fun build(context: Context): SipEngine = PjsipManager(context)
  }
}