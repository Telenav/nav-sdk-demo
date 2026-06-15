package com.telenav.sdk.demo.search

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class AndroidNetworkConnectivityProvider(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val callbacks = mutableSetOf<(Boolean) -> Unit>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = notify(true)
        override fun onLost(network: Network) = notify(isNetworkConnected())
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            notify(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        }
    }

    init {
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            networkCallback
        )
    }

    fun isNetworkConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun observeNetworkState(callback: (Boolean) -> Unit) {
        callbacks.add(callback)
        callback(isNetworkConnected())
    }

    fun removeNetworkObserver(callback: (Boolean) -> Unit) {
        callbacks.remove(callback)
    }

    fun release() {
        callbacks.clear()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }

    private fun notify(connected: Boolean) {
        callbacks.toList().forEach { it(connected) }
    }
}
