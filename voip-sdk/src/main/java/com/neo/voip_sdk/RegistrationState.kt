package com.neo.voip_sdk

sealed class RegistrationState {
    object None : RegistrationState()
    object Progress : RegistrationState()
    object Registered : RegistrationState()
    object Cleared : RegistrationState()
    object Refreshing : RegistrationState()
    data class Failed(val message : String?) : RegistrationState()
}
