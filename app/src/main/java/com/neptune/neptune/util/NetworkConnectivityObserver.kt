package com.neptune.neptune.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.neptune.neptune.NepTuneApplication
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Utility for observing the device's internet connectivity status in real time.
 *
 * This class relies on Android's [ConnectivityManager] to detect when the
 * connection is established, lost, or unavailable.
 *
 * @author Grégory Blanc This class was made using AI assistance.
 */
class NetworkConnectivityObserver {
  private val connectivityManager =
      NepTuneApplication.appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
          as ConnectivityManager

  val isOnline: Flow<Boolean> =
      callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                  override fun onAvailable(network: Network) {
                    trySend(true)
                  }

                  override fun onLost(network: Network) {
                    trySend(false)
                  }

                  override fun onUnavailable() {
                    trySend(false)
                  }
                }

            val request =
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()

            connectivityManager.registerNetworkCallback(request, callback)

            val current = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(current)
            val isConnected =
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            trySend(isConnected)

            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
          }
          .distinctUntilChanged()
}
