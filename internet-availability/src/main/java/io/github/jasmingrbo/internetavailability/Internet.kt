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

import android.app.Application
import android.content.Context
import android.net.Network
import io.github.jasmingrbo.internetavailability.common.collectSubscriptionCount
import io.github.jasmingrbo.internetavailability.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Class that allows internet availability observation. It's a singleton so there can only ever
 * be one instance of this class. The intention is to be instantiated in the project's
 * [Application] class.
 *
 * @property scope A [CoroutineScope] in which all the asynchronous work is executed. Intended
 * scope to be used is [Application]'s scope.
 * @property pingUrl A URL that's going to be pinged in intervals of
 * [nonMeteredNetworksPingIntervalMillis] and [meteredNetworksPingIntervalMillis].
 * @property pingTimeoutMillis Time in milliseconds after which the ongoing ping to the
 * [pingUrl] will fail.
 * @property pingRetryTimes Times to retry current network pinging before reporting the failure.
 * @property pingRetryIntervalMillis Delay between each network pinging retry.
 * @param pingOnNonMeteredNetworks Whether pinging should be done on non-metered networks or not
 * @property nonMeteredNetworksPingIntervalMillis Pinging interval for non-metered all networks.
 * Meaning all non-metered networks will be pinged in parallel after this interval has passed
 * counting from the last time these networks were pinged.
 * @property pingOnMeteredNetworks Whether the pinging should be done on metered networks or not.
 * @property meteredNetworksPingIntervalMillis Pinging interval for metered all networks. All
 * metered networks will be pinged in parallel after this interval has passed counting from the
 * last time these networks were pinged.
 * @param context [Application] context.
 *
 * @throws [IllegalArgumentException] if [pingUrl] isn't a valid HTTP or HTTPS URL or if no address
 * is associated with it.
 */
class Internet private constructor(
    private val scope: CoroutineScope,
    private val pingUrl: String,
    internal val pingTimeoutMillis: Int,
    internal val pingRetryTimes: Int,
    internal val pingRetryIntervalMillis: Long,
    internal val pingOnNonMeteredNetworks: Boolean,
    internal val nonMeteredNetworksPingIntervalMillis: Long,
    internal val pingOnMeteredNetworks: Boolean,
    internal val meteredNetworksPingIntervalMillis: Long,
    context: Context
) {
    private var lastNetworksChangePingAtMillis: Long = 0

    private val pinger = Pinger(
        url = pingUrl,
        timeoutMillis = pingTimeoutMillis,
        retryTimes = pingRetryTimes,
        retryIntervalMillis = pingRetryIntervalMillis
    )

    private var observingNetworks: Job? = null
    private var pinging: Job? = null

    // Using SharedFlow instead of StateFlow, because SharedFlow waits for all collectors to
    // consume emitted value before emitting new values.
    private val _availability = MutableSharedFlow<Boolean>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Internet availability stream, true value means internet is available whereas false
     * indicates the absence of internet. Producing is started upon first collector and stopped
     * 5 seconds after the last collector completes.
     */
    val availability: StateFlow<Boolean> = _availability.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    private val _pings = mutableListOf<Unit>()

    /**
     * Pings list for tracking pings that were made. Added purely for testing purposes.
     */
    internal val pings: List<Unit> = _pings

    private val networkManager = InternetNetworkManager(scope = scope, context = context)

    private var networksSnapshot = emptyList<Network>() to lazy { emptyList<Network>() }

    private val Map<Network, Boolean>.nonMetered: List<Network>
        get() = mapNotNull { network -> if (network.value) network.key else null }

    private val Map<Network, Boolean>.metered: List<Network>
        get() = mapNotNull { network -> if (!network.value) network.key else null }

    init {
        _availability.collectSubscriptionCount(
            scope = scope,
            onCountGreaterThanZero = ::onAvailabilityCollected,
            onCountZero = ::onAvailabilityCollectionCompleted
        )
    }

    private fun onAvailabilityCollected() {
        Logger.d("availability is being collected")
        observingNetworks =
            scope.launch(Dispatchers.IO) {
                var firstCollection = true
                networkManager.networks.collectLatest { networks ->
                    // Adjust time to populate networks
                    if (firstCollection) {
                        firstCollection = false
                        delay(250)
                    }

                    Logger.d("Collected: $networks")
                    stopPinging()
                    val difference = System.currentTimeMillis() - lastNetworksChangePingAtMillis
                    if (difference < MIN_NETWORKS_CHANGE_PING_INTERVAL_MILLIS) {
                        delay(MIN_NETWORKS_CHANGE_PING_INTERVAL_MILLIS - difference)
                    }
                    emit(networks)
                    lastNetworksChangePingAtMillis = System.currentTimeMillis()
                    startPinging()
                }
            }
    }

    private fun onAvailabilityCollectionCompleted() {
        Logger.d("availability collection completed")
        observingNetworks?.cancel()
        stopPinging()
    }

    private suspend fun emit(networks: Map<Network, Boolean>) = emit(
        when {
            networks.isEmpty() -> {
                networksSnapshot = emptyList<Network>() to lazy { emptyList() }
                false
            }
            networks.containsValue(true) -> {
                networksSnapshot = networks.nonMetered to lazy { networks.metered }
                networksSnapshot.first.haveInternet() || networksSnapshot.second.value.haveInternet()
            }
            else -> {
                networksSnapshot = emptyList<Network>() to lazy { networks.metered }
                networksSnapshot.second.value.haveInternet()
            }
        }
    )

    private fun emit(hasInternet: Boolean) {
        _availability.tryEmit(hasInternet)
        Logger.d("Emitted: $hasInternet")
    }

    private suspend fun emitDelayed() {
        if (pingOnNonMeteredNetworks && pingOnMeteredNetworks) {
            Logger.d("About to ping all networks")
            coroutineScope {
                val doNonMeteredNetworksHaveInternet = async {
                    delay(nonMeteredNetworksPingIntervalMillis)
                    networksSnapshot.first.haveInternet()
                }

                val doMeteredNetworksHaveInternet = async(start = CoroutineStart.LAZY) {
                    if (meteredNetworksPingIntervalMillis > nonMeteredNetworksPingIntervalMillis) {
                        delay(meteredNetworksPingIntervalMillis - nonMeteredNetworksPingIntervalMillis)
                    }
                    networksSnapshot.second.value.haveInternet()
                }

                emit(doNonMeteredNetworksHaveInternet.await() || doMeteredNetworksHaveInternet.await())
                // coroutineScope won't return if it's not cancelled, since it was never needed
                if (!doMeteredNetworksHaveInternet.isActive) doMeteredNetworksHaveInternet.cancel()
            }
        } else if (pingOnNonMeteredNetworks) {
            Logger.d("About to ping non-metered networks")
            delay(nonMeteredNetworksPingIntervalMillis)
            emit(networksSnapshot.first.haveInternet())
        } else if (pingOnMeteredNetworks) {
            Logger.d("About to ping metered networks")
            delay(meteredNetworksPingIntervalMillis)
            emit(networksSnapshot.second.value.haveInternet())
        }
    }

    private suspend fun List<Network>.haveInternet(): Boolean = coroutineScope {
        map { network -> async { pinger.ping(network, availability.value) } }
            .awaitAll()
            .any { hasInternet -> hasInternet }
    }

    private fun CoroutineScope.startPinging() {
        pinging = launch {
            while ((pingOnNonMeteredNetworks || pingOnMeteredNetworks) && isActive) {
                val shouldPing =
                    (pingOnNonMeteredNetworks && pingOnMeteredNetworks && networkManager.networks.value.isNotEmpty())
                            || (pingOnNonMeteredNetworks && !pingOnMeteredNetworks && networksSnapshot.first.isNotEmpty())
                            || (!pingOnNonMeteredNetworks && pingOnMeteredNetworks && networksSnapshot.second.value.isNotEmpty())
                if (shouldPing) {
                    emitDelayed()
                    _pings.add(Unit)
                    Logger.d("pings updated")
                } else {
                    delay(100)
                }
            }
        }
    }

    private fun stopPinging() {
        pinging?.cancel()
    }

    /**
     * A companion object of [Internet] allowing you to grab its singleton instance.
     */
    companion object {
        @Volatile
        private var instance: Internet? = null

        /**
         * Returns an [Internet] instance if it's already instantiated otherwise it creates it
         * beforehand.
         *
         * @param scope A [CoroutineScope] in which all the asynchronous work is executed. Intended
         * scope to be used is [Application]'s scope.
         * @param pingUrl A URL that's going to be pinged in intervals of
         * [nonMeteredNetworksPingIntervalMillis] and [meteredNetworksPingIntervalMillis].
         * @param pingTimeoutMillis Time in milliseconds after which the ongoing ping to the
         * [pingUrl] will fail. Coerced to at least 1000ms.
         * @param pingRetryTimes Times to retry current network pinging before reporting the failure.
         * If > 0 coerced to at most 5 times else coerced to at least 0 times.
         * @param pingRetryIntervalMillis Delay between each network pinging retry. If
         * [pingRetryTimes] > 0 coerced to at least 1000ms.
         * @param pingOnNonMeteredNetworks Whether pinging should be done on non-metered networks or not
         * @param nonMeteredNetworksPingIntervalMillis Pinging interval for non-metered networks.
         * Meaning all non-metered networks will be pinged in parallel after this interval has passed
         * counting from the last time these networks were pinged. Coerced to at least 1000ms.
         * @param pingOnMeteredNetworks Whether the pinging should be done on metered networks or not.
         * @param meteredNetworksPingIntervalMillis Pinging interval for metered networks. All
         * metered networks will be pinged in parallel after this interval has passed counting from the
         * last time these networks were pinged. Coerced to at least 1000ms.
         * @param context [Application] context.
         *
         * @throws [IllegalArgumentException] if [pingUrl] isn't a valid HTTP or HTTPS URL or if
         * no address is associated with it.
         *
         * @return An [Internet] singleton instance.
         */
        fun getInstance(
            scope: CoroutineScope,
            pingUrl: String = "https://www.google.com",
            pingTimeoutMillis: Int = 2_000,
            pingRetryTimes: Int = 0,
            pingRetryIntervalMillis: Long = 0,
            pingOnNonMeteredNetworks: Boolean = true,
            nonMeteredNetworksPingIntervalMillis: Long = 10_000,
            pingOnMeteredNetworks: Boolean = true,
            meteredNetworksPingIntervalMillis: Long = nonMeteredNetworksPingIntervalMillis,
            context: Context
        ) = instance ?: synchronized(this) {
            instance ?: Internet(
                scope = scope,
                pingUrl = pingUrl,
                pingTimeoutMillis = pingTimeoutMillis.coerceAtLeast(MIN_PING_TIMEOUT_MILLIS),
                pingRetryTimes = if (pingRetryTimes > 0) {
                    pingRetryTimes.coerceAtMost(MAX_PING_RETRY_TIMES)
                } else {
                    0
                },
                pingRetryIntervalMillis = if (pingRetryTimes > 0) {
                    pingRetryIntervalMillis.coerceAtLeast(MIN_PING_RETRY_INTERVAL_MILLIS)
                } else {
                    0
                },
                pingOnNonMeteredNetworks = pingOnNonMeteredNetworks,
                nonMeteredNetworksPingIntervalMillis = nonMeteredNetworksPingIntervalMillis.coerceAtLeast(
                    MIN_PING_INTERVAL_MILLIS
                ),
                pingOnMeteredNetworks = pingOnMeteredNetworks,
                meteredNetworksPingIntervalMillis = meteredNetworksPingIntervalMillis.coerceAtLeast(
                    MIN_PING_INTERVAL_MILLIS
                ),
                context = context
            ).also { internet -> instance = internet }
        }

        /**
         * Clears the current singleton instance so that a call to the [getInstance] creates a new
         * one. Added purely for testing purposes.
         */
        internal fun clearInstance() {
            instance = null
        }

        /**
         * Fake proxy added for testing purposes. Used to route pinging through it in order
         * simulate internet cut off while still being connected to either wifi or mobile data
         * network.
         */
        internal const val TEST_PROXY = "127.0.0.1:8080"

        private const val MIN_PING_TIMEOUT_MILLIS = 1_000

        private const val MAX_PING_RETRY_TIMES = 5

        private const val MIN_PING_RETRY_INTERVAL_MILLIS: Long = 1_000

        private const val MIN_PING_INTERVAL_MILLIS: Long = 1_000

        private const val MIN_NETWORKS_CHANGE_PING_INTERVAL_MILLIS: Long = 500
    }
}