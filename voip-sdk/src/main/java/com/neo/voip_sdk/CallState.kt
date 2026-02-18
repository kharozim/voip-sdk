package com.neo.voip_sdk

sealed class CallState {
    object Idle : CallState()
    object Calling : CallState()
    object Ringing : CallState()
    object Connected : CallState()
    object Active : CallState()
    object Hold : CallState()
    object Disconnected : CallState()
    data class Error(val reason: String) : CallState()
}
