package com.neo.voip_sdk

sealed class RegistrationState {
    object None : RegistrationState()
    object Registering : RegistrationState()
    object Registered : RegistrationState()
    object Failed : RegistrationState()
}
