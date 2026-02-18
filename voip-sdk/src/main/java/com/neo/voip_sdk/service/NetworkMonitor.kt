package com.neo.voip_sdk.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network

class NetworkMonitor(
    context: Context,
    private val onNetworkAvailable: () -> Unit
) {

    private val cm =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onNetworkAvailable()
        }
    }

    fun start() {
        cm.registerDefaultNetworkCallback(callback)
    }

    fun stop() {
        cm.unregisterNetworkCallback(callback)
    }
}
