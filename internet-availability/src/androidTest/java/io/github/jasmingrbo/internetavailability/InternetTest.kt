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
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jasmingrbo.internetavailability.helpers.Awaiter.await
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.disableNetworks
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableInternet
import io.github.jasmingrbo.internetavailability.helpers.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.junit.After
import org.junit.Before
import kotlin.math.max

/**
 * A base testing class for differently set up [Internet] instances.
 */
internal abstract class InternetTest {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    protected val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    protected lateinit var internet: Internet
    protected val collectedInternetAvailabilities = mutableListOf<Boolean>()
    private lateinit var collector: Job
    private var maxPingAwaitTimeMillis: Long? = null

    abstract fun provideInternet(): Internet

    init {
        disableNetworks()
    }

    @Before
    fun initInternet() {
        internet = provideInternet()
    }

    @After
    fun releaseResources() {
        collector.cancel()
        disableNetworks()
        enableInternet()
        collectedInternetAvailabilities.clear()
        Internet.clearInstance()
    }

    protected fun collectInternetAvailability() {
        collector = internet.availability
            .collectLatest(collector = collectedInternetAvailabilities, scope = scope)
            .also { await(1_000) }
    }

    protected fun cancelInternetAvailabilityCollection() {
        collector.cancel()
        await(6_000)
    }

    protected fun awaitPing(coerceAtMost: Long? = null) {
        maxPingAwaitTimeMillis?.let { pingAwaitTime ->
            await(pingAwaitTime)
        } ?: initMaxPingAwaitTimeMillis(coerceAtMost).also { pingAwaitTime ->
            await(pingAwaitTime)
        }
    }

    private fun initMaxPingAwaitTimeMillis(coerceAtMost: Long? = null): Long {
        return with(internet) {
            val base = if (pingOnNonMeteredNetworks && pingOnMeteredNetworks) {
                max(
                    nonMeteredNetworksPingIntervalMillis,
                    meteredNetworksPingIntervalMillis
                )
            } else if (pingOnNonMeteredNetworks) {
                nonMeteredNetworksPingIntervalMillis
            } else if (pingOnMeteredNetworks) {
                meteredNetworksPingIntervalMillis
            } else {
                0
            }
            if (base != 0L) {
                val awaitTime =
                    base + pingRetryTimes * (pingTimeoutMillis + pingRetryIntervalMillis) + 1_000
                if (awaitTime < 0) Long.MAX_VALUE else awaitTime
            } else {
                base
            }
        }.let { pingAwaitTime ->
            (coerceAtMost?.let { maxAwaitTime ->
                pingAwaitTime.coerceAtMost(maxAwaitTime)
            } ?: pingAwaitTime).also { awaitTime ->
                maxPingAwaitTimeMillis = awaitTime
            }
        }
    }
}