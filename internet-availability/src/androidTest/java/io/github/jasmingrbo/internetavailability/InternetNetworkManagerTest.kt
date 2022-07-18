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
import io.github.jasmingrbo.internetavailability.helpers.Awaiter.await
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.disableMobileData
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.disableNetworks
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.disableWifi
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableMobileData
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableNetworks
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableWifi
import io.github.jasmingrbo.internetavailability.helpers.collectLatestWithDelayOnFirstCollection
import io.github.jasmingrbo.internetavailability.helpers.equalsOneOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class InternetNetworkManagerTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var networkManager: InternetNetworkManager
    private val collectedNetworks = mutableListOf<List<Boolean>>()
    private lateinit var collector: Job

    init {
        disableNetworks()
    }

    @Before
    fun initNetworkManager() {
        networkManager = InternetNetworkManager(scope = scope, context = context)
    }

    @After
    fun releaseResources() {
        collector.cancel()
        disableNetworks()
        collectedNetworks.clear()
    }

    private fun collectNetworks() {
        collector = networkManager.networks
            .map { networks -> networks.map { network -> network.value } }
            .collectLatestWithDelayOnFirstCollection(collector = collectedNetworks, scope = scope)
            .also { await(1_000) }
    }

    private fun cancelNetworksCollection() {
        collector.cancel()
        await(1_000)
    }

    @Test
    fun given_wifi_and_mobile_data_when_networks_not_collected_and_afterwards_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance
        enableNetworks()

        // When networkManager.networks isn't collected
        // Then
        assert(collectedNetworks.isEmpty())
        assert(networkManager.networks.value.isEmpty())
        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableWifi()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableWifi()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableNetworks()
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertEquals(
            listOf(listOf(true), listOf(false), listOf(false, true), listOf(false), listOf()),
            collectedNetworks
        )
    }

    @Test
    fun given_mobile_data_and_wifi_when_networks_not_collected_and_afterwards_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance
        enableNetworks(wifiFirst = false)

        // When networkManager.networks isn't collected
        // Then
        assert(collectedNetworks.isEmpty())
        assert(networkManager.networks.value.isEmpty())
        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableMobileData()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableMobileData()
        disableWifi()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableWifi()
        disableMobileData()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableWifi()
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertThat(
            collectedNetworks,
            equalsOneOf(
                listOf(
                    listOf(false),
                    listOf(false, true),
                    listOf(true),
                    listOf(false),
                    listOf(false, true),
                    listOf(true),
                    listOf()
                ),
                listOf(
                    listOf(true),
                    listOf(true, false),
                    listOf(true),
                    listOf(false),
                    listOf(false, true),
                    listOf(true),
                    listOf()
                )
            )
        )
    }

    @Test
    fun given_neither_wifi_nor_mobile_data_when_networks_not_collected_and_afterwards_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance and neither wifi nor mobile data

        // When networkManager.networks isn't collected
        // Then
        assert(collectedNetworks.isEmpty())
        assert(networkManager.networks.value.isEmpty())
        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isEmpty())
        // When
        enableWifi()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableWifi()
        // Then
        assert(networkManager.networks.value.isEmpty())
        // When
        enableMobileData()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableWifi()
        disableNetworks(wifiFirst = false)
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertEquals(
            listOf(
                listOf(),
                listOf(true),
                listOf(),
                listOf(false),
                listOf(false, true),
                listOf(true),
                listOf()
            ),
            collectedNetworks
        )
    }

    @Test
    fun given_wifi_when_networks_not_collected_and_afterwards_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance
        enableWifi()

        // When networkManager.networks isn't collected
        // Then
        assert(collectedNetworks.isEmpty())
        assert(networkManager.networks.value.isEmpty())
        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableWifi()
        // Then
        assert(networkManager.networks.value.isEmpty())
        // When
        enableNetworks(wifiFirst = false)
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableNetworks()
        // Then
        assert(networkManager.networks.value.isEmpty())
        // When
        enableMobileData()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableWifi()
        disableNetworks(wifiFirst = false)
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertEquals(
            listOf(
                listOf(true),
                listOf(),
                listOf(false),
                listOf(false, true),
                listOf(false),
                listOf(),
                listOf(false),
                listOf(false, true),
                listOf(true),
                listOf()
            ),
            collectedNetworks
        )
    }

    @Test
    fun given_mobile_data_when_networks_not_collected_and_afterwards_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance
        enableMobileData()

        // When networkManager.networks isn't collected
        // Then
        assert(collectedNetworks.isEmpty())
        assert(networkManager.networks.value.isEmpty())
        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableMobileData()
        // Then
        assert(networkManager.networks.value.isEmpty())
        // When
        enableMobileData()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableWifi()
        disableNetworks(wifiFirst = false)
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertEquals(
            listOf(
                listOf(false),
                listOf(),
                listOf(false),
                listOf(false, true),
                listOf(true),
                listOf()
            ),
            collectedNetworks
        )
    }

    @Test
    fun given_wifi_and_mobile_data_when_networks_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance
        enableNetworks()

        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableWifi()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableWifi()
        disableMobileData()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableMobileData()
        disableNetworks()
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertEquals(
            listOf(
                listOf(true),
                listOf(false),
                listOf(false, true),
                listOf(true),
                listOf(false),
                listOf()
            ),
            collectedNetworks
        )
    }

    @Test
    fun given_mobile_data_and_wifi_when_networks_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance
        enableNetworks(wifiFirst = false)

        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableNetworks(wifiFirst = false)
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isEmpty())
        // When
        enableMobileData()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableWifi()
        disableNetworks()
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertThat(
            collectedNetworks,
            equalsOneOf(
                listOf(
                    listOf(false),
                    listOf(false, true),
                    listOf(true),
                    listOf(),
                    listOf(false),
                    listOf(false, true),
                    listOf(false),
                    listOf()
                ),
                listOf(
                    listOf(true),
                    listOf(true, false),
                    listOf(true),
                    listOf(),
                    listOf(false),
                    listOf(false, true),
                    listOf(false),
                    listOf()
                ),
                // In rare cases it skips the 2nd element as well, no harm and is better than
                // taking the 1st element, which also rarely happens if the delay is shorter
                listOf(
                    listOf(false, true),
                    listOf(true),
                    listOf(),
                    listOf(false),
                    listOf(false, true),
                    listOf(false),
                    listOf()
                ),
                listOf(
                    listOf(true, false),
                    listOf(true),
                    listOf(),
                    listOf(false),
                    listOf(false, true),
                    listOf(false),
                    listOf()
                )
            )
        )
    }

    @Test
    fun given_neither_wifi_nor_mobile_data_when_networks_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance and neither wifi nor mobile data

        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isEmpty())
        // When
        enableNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableWifi()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableMobileData()
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertEquals(
            listOf(listOf(), listOf(true), listOf(false), listOf()),
            collectedNetworks
        )
    }

    @Test
    fun given_wifi_when_networks_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance
        enableWifi()

        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableWifi()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isEmpty())
        // When
        enableWifi()
        disableWifi()
        // Then
        assert(networkManager.networks.value.isEmpty())
        // When
        enableNetworks(wifiFirst = false)
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableNetworks()
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertEquals(
            listOf(
                listOf(true),
                listOf(),
                listOf(true),
                listOf(),
                listOf(false),
                listOf(false, true),
                listOf(false),
                listOf()
            ),
            collectedNetworks
        )
    }

    @Test
    fun given_mobile_data_when_networks_collected_and_toggled_then_correctly_emitted_networks() {
        // Given networkManager instance
        enableMobileData()

        // When
        collectNetworks()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableWifi()
        // Then
        assert(collectedNetworks.isNotEmpty())
        assert(networkManager.networks.value.isNotEmpty())
        // When
        disableNetworks(wifiFirst = false)
        // Then
        assert(networkManager.networks.value.isEmpty())
        // When
        enableWifi()
        // Then
        assert(networkManager.networks.value.isNotEmpty())
        // When
        enableMobileData()
        disableNetworks()
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertEquals(
            listOf(
                listOf(false),
                listOf(false, true),
                listOf(true),
                listOf(),
                listOf(true),
                listOf(false),
                listOf()
            ),
            collectedNetworks
        )
    }

    @Test
    fun given_wifi_and_mobile_data_when_networks_collected_and_collection_cancelled_then_correctly_emitted_networks() {
        // Given networkManager instance
        enableNetworks()

        // When
        collectNetworks()
        // Then
        assert(networkManager.networks.value.isNotEmpty())

        // When
        cancelNetworksCollection()
        // Then
        assert(networkManager.networks.value.isEmpty())
        assertEquals(listOf(listOf(true)), collectedNetworks)
    }
}