/*
 * Copyright 2022 Jasmin Grbo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jasmingrbo.internetavailability

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import io.github.jasmingrbo.internetavailability.common.collectSubscriptionCount
import io.github.jasmingrbo.internetavailability.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class InternetNetworkManager(
    private val scope: CoroutineScope,
    context: Context
) {
    private val validNetworks = ValidNetworks()

    private var emitter: Job? = null

    private val _networks = MutableStateFlow<Map<Network, Boolean>>(emptyMap())
    internal val networks = _networks.asStateFlow()

    private val connectivityManager: ConnectivityManager = getConnectivityManager(context)

    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    // Note: onAvailable is not called when cellular (metered) is turned on if the
    // wifi (non-metered) was already on, it is called immediately after wifi is turned off and
    // its onLost is done, so we get empty networks for a brief moment. Smooth transition
    // implemented by introducing a 1_000ms delay when networks is empty and it previously
    // contained wifi.
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            validNetworks.add(network)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            validNetworks.remove(network)
        }
    }

    private val Network.isNonMetered: Boolean
        get() = connectivityManager.getNetworkCapabilities(this)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: false

    init {
        _networks.collectSubscriptionCount(
            scope = scope,
            onCountGreaterThanZero = ::onNetworksCollected,
            onCountZero = ::onNetworksCollectionCompleted
        )
    }

    private fun onNetworksCollected() {
        Logger.d("networks is being collected")
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun onNetworksCollectionCompleted() {
        Logger.d("networks' collection completed")
        connectivityManager.unregisterNetworkCallback(networkCallback)
        validNetworks.clear()
    }

    private fun emit(networks: Map<Network, Boolean>, delay: Boolean) {
        emitter?.cancel()
        emitter = scope.launch(Dispatchers.IO) {
            if (delay) delay(1_000)
            _networks.value = networks
            Logger.d("Emitted: $networks")
        }
    }

    private fun getConnectivityManager(context: Context): ConnectivityManager {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        } else {
            context.getSystemService(ConnectivityManager::class.java)
        }
    }

    private inner class ValidNetworks {
        private val validNetworks = mutableMapOf<Network, Boolean>()

        fun add(network: Network) {
            validNetworks[network] = network.isNonMetered
            Logger.d("Network added: $network")
            emit()
        }

        fun remove(network: Network) {
            val delay = validNetworks.getValue(network)
            validNetworks.remove(network)
            Logger.d("Network removed: $network")
            emit(delay && validNetworks.isEmpty())
        }

        fun clear() {
            validNetworks.clear()
            Logger.d("Networks cleared out")
            emit()
        }

        private fun emit(delay: Boolean = false) {
            emit(validNetworks.toMap(), delay) // .toMap() otherwise it'll consider values the same
        }
    }
}