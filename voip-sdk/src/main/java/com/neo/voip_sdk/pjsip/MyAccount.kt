package com.neo.voip_sdk.pjsip

import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.OnIncomingCallParam
import org.pjsip.pjsua2.OnRegStateParam

/**
 * Custom Account wrapper for PJSUA2.
 * - Captures registration state changes
 * - Captures incoming calls
 *
 * Usage:
 *   val acc = MyAccount(endpoint, callbacks)
 *   acc.create(accCfg)
 */
class MyAccount(
    private val endpoint: Endpoint,
    private val callbacks: Callbacks,
) : Account() {

    interface Callbacks {
        fun onRegistration(active: Boolean, code: Int, reason: String)
        fun onIncoming(call: MyCall)
    }

    override fun onRegState(prm: OnRegStateParam?) {
        super.onRegState(prm)
        val info = runCatching { this.info }.getOrNull()
        val active = info?.regIsActive ?: false

        // SWIG constant-int style
        val code = prm?.code ?: 0
        val reason = prm?.reason ?: ""

        callbacks.onRegistration(active, code, reason)
    }

    override fun onIncomingCall(prm: OnIncomingCallParam?) {
        super.onIncomingCall(prm)
        val callId = prm?.callId ?: -1
        val call = MyCall(this, endpoint, callbacks = null, callId = callId)
        callbacks.onIncoming(call)
    }

    fun getDomainFromIdUri(): String? {
        val uri = runCatching { this.info.uri }.getOrNull() ?: return null
        return uri.substringAfter("@", "")
            .substringBefore(">", "")
            .substringBefore(";", "")
            .ifBlank { null }
    }
}