package com.neo.voip_sdk.pjsip

import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.OnCallMediaStateParam
import org.pjsip.pjsua2.OnCallStateParam
import org.pjsip.pjsua2.pjmedia_type
import org.pjsip.pjsua2.pjsip_inv_state
import org.pjsip.pjsua2.pjsip_status_code
import org.pjsip.pjsua2.pjsua_call_media_status

/**
 * Custom Call wrapper for PJSUA2.
 * - Captures call state changes
 * - Captures media state changes and connects audio media when ACTIVE
 *
 * Usage:
 *   val call = MyCall(account, endpoint, callbacks)
 *   call.makeCall(destUri, CallOpParam(true))
 */
class MyCall(
  acc: Account,
  private val endpoint: Endpoint,
  private val callbacks: Callbacks? = null,
  callId: Int = -1,
) : Call(acc, callId) {

  interface Callbacks {
    fun onCallState(state: Int, code: Int, reason: String)
    fun onMediaState(active: Boolean)
  }

  override fun onCallState(prm: OnCallStateParam?) {
    super.onCallState(prm)
    val ci = runCatching { this.info }.getOrNull()

    // SWIG constant-int style
    val state = ci?.state ?: pjsip_inv_state.PJSIP_INV_STATE_NULL
    val code = ci?.lastStatusCode ?: 0
    val reason = ci?.lastReason ?: ""

    callbacks?.onCallState(state, code, reason)
  }

  override fun onCallMediaState(prm: OnCallMediaStateParam?) {
    super.onCallMediaState(prm)
    try {
      val ci = this.info
      for (i in 0 until ci.media.size) {
        val mi = ci.media[i]
        if (mi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
          mi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
        ) {
          val audioMedia = getAudioMedia(i)
          val adm = endpoint.audDevManager()
          val cap = adm.captureDevMedia
          val play = adm.playbackDevMedia

          cap.startTransmit(audioMedia)
          audioMedia.startTransmit(play)

          callbacks?.onMediaState(true)
          return
        }
      }
      callbacks?.onMediaState(false)
    } catch (_: Throwable) {
      callbacks?.onMediaState(false)
    }
  }

  fun safeHangup() {
    runCatching {
      val prm = CallOpParam(true).apply {
        statusCode = pjsip_status_code.PJSIP_SC_DECLINE
      }
      hangup(prm)
    }.onFailure {
      runCatching { hangup(CallOpParam()) }
    }
  }

  fun remoteUriSafe(): String =
    runCatching { this.info.remoteUri }.getOrDefault("unknown")
}