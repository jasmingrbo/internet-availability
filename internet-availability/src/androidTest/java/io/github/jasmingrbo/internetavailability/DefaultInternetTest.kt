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
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.disableInternet
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.disableMobileData
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.disableNetworks
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.disableWifi
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableInternet
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableMobileData
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableNetworks
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableWifi
import io.github.jasmingrbo.internetavailability.helpers.equalsOneOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class DefaultInternetTest : InternetTest() {
    override fun provideInternet() = Internet.getInstance(scope = scope, context = context)

    @Test
    fun given_wifi_and_mobile_data_when_availability_not_collected_and_afterwards_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance
        enableNetworks()

        // When internet.availability isn't collected
        // Then
        assert(collectedInternetAvailabilities.isEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.availability.value)
        // When
        disableWifi()
        // Then
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(!internet.availability.value)
        // When
        enableNetworks(wifiFirst = false)
        // Then
        assert(internet.availability.value)
        // When
        disableNetworks()
        // Then
        assert(internet.pings.isEmpty())
        assert(!internet.availability.value)
        assertEquals(listOf(true, false, true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_mobile_data_and_wifi_when_availability_not_collected_and_afterwards_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance
        enableNetworks(wifiFirst = false)

        // When internet.availability isn't collected
        // Then
        assert(collectedInternetAvailabilities.isEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.availability.value)
        // When
        disableWifi()
        // Then
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(!internet.availability.value)
        // When
        enableMobileData()
        // Then
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(!internet.availability.value)
        // When
        enableWifi()
        // Then
        assert(internet.availability.value)
        // When
        enableMobileData()
        disableNetworks()
        // Then
        assert(!internet.availability.value)
        // When
        enableWifi()
        // Then
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        assertEquals(
            listOf(true, false, true, false, true, false, true),
            collectedInternetAvailabilities
        )
    }

    @Test
    fun given_neither_wifi_nor_mobile_data_when_availability_not_collected_and_afterwards_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance and neither wifi nor mobile data

        // When internet.availability isn't collected
        // Then
        assert(collectedInternetAvailabilities.isEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(!internet.availability.value)
        // When
        enableWifi()
        // Then
        assert(internet.availability.value)
        // When
        disableWifi()
        // Then
        assert(!internet.availability.value)
        // When
        enableNetworks()
        // Then
        assert(internet.availability.value)
        // When
        disableWifi()
        // Then
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(internet.pings.isEmpty())
        assert(!internet.availability.value)
        assertEquals(listOf(true, false, true, false, true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_wifi_when_availability_not_collected_and_afterwards_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance
        enableWifi()

        // When internet.availability isn't collected
        // Then
        assert(collectedInternetAvailabilities.isEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.availability.value)
        // When
        enableMobileData()
        disableWifi()
        // Then
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(!internet.availability.value)
        // When
        enableNetworks()
        // Then
        assert(internet.availability.value)
        // When
        disableNetworks(wifiFirst = false)
        // Then
        // Because we enabled first wifi then mobile, mobile did not get caught and we waited
        // ~10s (5s + 5s - time wifi got awaken) then disabling first mobile another 5s waiting,
        // all together it can be enough for a ping to go through
        assertThat(internet.pings.isEmpty(), equalsOneOf(true, false))
        assert(!internet.availability.value)
        assertEquals(listOf(true, false, true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_mobile_data_when_availability_not_collected_and_afterwards_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance
        enableMobileData()

        // When internet.availability isn't collected
        // Then
        assert(collectedInternetAvailabilities.isEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(!internet.availability.value)
        // When
        enableWifi()
        // Then
        assert(internet.availability.value)
        // When
        enableMobileData()
        disableNetworks()
        // Then
        assert(!internet.availability.value)
        // When
        enableMobileData()
        // Then
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        assertEquals(listOf(true, false, true, false, true), collectedInternetAvailabilities)
    }

    @Test
    fun given_wifi_and_mobile_data_when_availability_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance
        enableNetworks()

        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        // When
        disableNetworks()
        // Then
        assert(!internet.availability.value)
        // When
        enableMobileData()
        // Then
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(!internet.availability.value)
        // When
        enableNetworks()
        // Then
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        assertEquals(listOf(true, false, true, false, true), collectedInternetAvailabilities)
    }

    @Test
    fun given_mobile_data_and_wifi_when_availability_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance
        enableNetworks(wifiFirst = false)

        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(internet.availability.value)
        // When
        enableMobileData()
        disableWifi()
        // Then
        assert(internet.availability.value)
        // When
        enableWifi()
        disableNetworks()
        // Then
        assert(internet.pings.isEmpty())
        assert(!internet.availability.value)
        assertEquals(listOf(true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_neither_wifi_nor_mobile_data_when_availability_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance and neither wifi nor mobile data

        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(!internet.availability.value)
        // When
        enableMobileData()
        // Then
        assert(internet.availability.value)
        // When
        enableWifi()
        disableMobileData()
        // Then
        assert(internet.availability.value)
        // When
        disableWifi()
        // Then
        assert(!internet.availability.value)
        // When
        enableNetworks()
        // Then
        assert(internet.availability.value)
        // When
        disableWifi()
        // Then
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(internet.pings.isEmpty())
        assert(!internet.availability.value)
        assertEquals(listOf(true, false, true, false, true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_wifi_when_availability_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance
        enableWifi()

        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        // When
        enableMobileData()
        disableWifi()
        // Then
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(internet.pings.isEmpty())
        assert(!internet.availability.value)
        assertEquals(listOf(true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_mobile_data_when_availability_collected_and_toggled_every_less_than_10_secs_then_correctly_emitted_availability_and_no_pinging() {
        // Given internet instance
        enableMobileData()

        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        // When
        enableWifi()
        disableMobileData()
        // Then
        assert(internet.availability.value)
        // When
        enableMobileData()
        disableWifi()
        // Then
        assert(internet.availability.value)
        // When
        disableMobileData()
        // Then
        assert(!internet.availability.value)
        // When
        enableNetworks(wifiFirst = false)
        // Then
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        assertEquals(listOf(true, false, true), collectedInternetAvailabilities)
    }

    @Test
    fun given_neither_wifi_nor_mobile_data_when_availability_collected_then_no_pinging() {
        // Given internet instance and neither wifi nor mobile data

        // When
        collectInternetAvailability()

        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assertEquals(listOf(true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_wifi_when_availability_collected_then_pinging() {
        // Given internet instance
        enableWifi()

        // When
        collectInternetAvailability()
        awaitPing()

        // Then
        // Cannot make assertion about how many because we're taking into consideration maximum
        // time it can take a ping to be successful or unsuccessful but if it's taken only the
        // minimum time there could've been multiple other minimum time pings made in the meantime
        val count = internet.pings.size
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isNotEmpty())

        // When
        awaitPing()

        // Then
        assert(internet.pings.size > count)
        assertEquals(listOf(true), collectedInternetAvailabilities)
    }

    @Test
    fun given_wifi_when_availability_collected_and_wifi_turned_off_then_pinging() {
        // Given internet instance
        enableWifi()

        // When
        collectInternetAvailability()
        awaitPing()
        // Then
        val count = internet.pings.size
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isNotEmpty())

        // When
        disableWifi()
        awaitPing()
        // Then
        assert(internet.pings.size == count)
        assertEquals(listOf(true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_mobile_data_when_availability_collected_then_pinging() {
        // Given internet instance
        enableMobileData()

        // When
        collectInternetAvailability()
        awaitPing()

        // Then
        val count = internet.pings.size
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isNotEmpty())

        // When
        awaitPing()

        // Then
        assert(internet.pings.size > count)
        assertEquals(listOf(true), collectedInternetAvailabilities)
    }

    @Test
    fun given_mobile_data_when_availability_collected_and_mobile_data_turned_off_then_pinging() {
        // Given internet instance
        enableMobileData()

        // When
        collectInternetAvailability()
        awaitPing()
        // Then
        val count = internet.pings.size
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isNotEmpty())

        // When
        disableMobileData()
        awaitPing()
        // Then
        assert(internet.pings.size == count)
        assertEquals(listOf(true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_wifi_and_mobile_data_when_availability_collected_and_internet_toggled_then_correctly_emitted_availability_and_pinging() {
        // Given internet instance
        enableNetworks()

        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)

        // When
        disableInternet()
        // Then
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)

        // When
        awaitPing()
        // Then
        val count = internet.pings.size
        assert(internet.pings.isNotEmpty())
        assert(!internet.availability.value)

        // When
        enableInternet()
        awaitPing()
        // Then
        assert(internet.pings.size > count)
        assert(internet.availability.value)
        assertEquals(listOf(true, false, true), collectedInternetAvailabilities)
    }

    @Test
    fun given_wifi_when_availability_collected_and_internet_toggled_then_correctly_emitted_availability_and_pinging() {
        // Given internet instance
        enableWifi()

        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)

        // When
        disableInternet()
        // Then
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)

        // When
        awaitPing()
        // Then
        val count = internet.pings.size
        assert(internet.pings.isNotEmpty())
        assert(!internet.availability.value)

        // When
        enableInternet()
        awaitPing()
        // Then
        assert(internet.pings.size > count)
        assert(internet.availability.value)
        assertEquals(listOf(true, false, true), collectedInternetAvailabilities)
    }

    @Test
    fun given_mobile_data_when_availability_collected_and_internet_toggled_then_correctly_emitted_availability_and_pinging() {
        // Given internet instance
        enableMobileData()

        // When
        collectInternetAvailability()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)

        // When
        disableInternet()
        // Then
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)

        // When
        awaitPing()
        // Then
        val count = internet.pings.size
        assert(internet.pings.isNotEmpty())
        assert(!internet.availability.value)

        // When
        enableInternet()
        awaitPing()
        // Then
        assert(internet.pings.size > count)
        assert(internet.availability.value)
        assertEquals(listOf(true, false, true), collectedInternetAvailabilities)
    }

    @Test
    fun given_mobile_data_and_wifi_when_availability_collected_and_collection_cancelled_then_correctly_emitted_availability() {
        // Given internet instance
        enableNetworks(wifiFirst = false)

        // When
        collectInternetAvailability()
        // Then
        assert(internet.availability.value)

        // When
        cancelInternetAvailabilityCollection()
        // Then
        assert(internet.availability.value)

        // When
        disableNetworks()
        // Then
        assert(internet.availability.value)
        assertEquals(listOf(true), collectedInternetAvailabilities)
    }
}