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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.disableNetworks
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableInternet
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableWifi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class PingerTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        disableNetworks()
    }

    @After
    fun releaseResources() {
        disableNetworks()
        enableInternet()
    }

    @Test(expected = IllegalArgumentException::class)
    fun given_pinger_with_wrong_url_protocol_then_exception() {
        // Given
        Pinger(
            url = "htt://www.google.com",
            timeoutMillis = 1_000,
            retryTimes = 0,
            retryIntervalMillis = 0
        )

        // When - pinger instantiated with wrong url

        // Then - exception thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun given_pinger_with_url_without_protocol_then_exception() {
        // Given
        Pinger(
            url = "www.google.com",
            timeoutMillis = 1_000,
            retryTimes = 0,
            retryIntervalMillis = 0
        )

        // When - pinger instantiated with wrong url

        // Then - exception thrown
    }

    @Test
    fun given_pinger_without_www_then_no_exception() {
        // Given
        Pinger(
            url = "https://google.com",
            timeoutMillis = 1_000,
            retryTimes = 0,
            retryIntervalMillis = 0
        )
    }

    @Test
    fun given_pinger_with_unknown_url_when_ping_not_invoked_then_no_exception() {
        // Given
        Pinger(
            url = "https://www.googledjahdjahkwww.com",
            timeoutMillis = 1_000,
            retryTimes = 0,
            retryIntervalMillis = 0
        )

        // When - ping not invoked

        // Then - no exception thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun given_pinger_with_wrong_url_when_pinged_then_exception(): Unit = runBlocking {
        // Given
        val networkManager = InternetNetworkManager(scope = scope, context = context)
        networkManager.networks.launchIn(scope)
        enableWifi()
        val pinger = Pinger(
            url = "https://www.googledjahdjahkwww.com",
            timeoutMillis = 1_000,
            retryTimes = 0,
            retryIntervalMillis = 0
        )

        // When
        pinger.ping(networkManager.networks.value.keys.first(), false)

        // Then - exception thrown
    }

    @Test
    fun given_pinger_with_correct_url_when_pinged_then_no_exception(): Unit = runBlocking {
        // Given
        val networkManager = InternetNetworkManager(scope = scope, context = context)
        networkManager.networks.launchIn(scope)
        enableWifi()
        val pinger = Pinger(
            url = "https://www.google.com",
            timeoutMillis = 1_000,
            retryTimes = 0,
            retryIntervalMillis = 0
        )

        // When
        pinger.ping(networkManager.networks.value.keys.first(), false)

        // Then - no exception thrown
    }
}