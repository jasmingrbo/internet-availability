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
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableInternet
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableMobileData
import io.github.jasmingrbo.internetavailability.helpers.NetworkController.enableWifi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class NonMeteredPingingInternetTest : InternetTest() {
    override fun provideInternet() = Internet.getInstance(
        scope = scope,
        pingOnMeteredNetworks = false,
        context = context
    )

    @Test
    fun given_neither_wifi_nor_mobile_data_when_availability_collected_then_no_pinging() {
        // Given internet instance and neither wifi nor mobile data

        // When
        collectInternetAvailability()
        awaitPing()

        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(!internet.availability.value)
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
    fun given_mobile_data_when_availability_collected_then_no_pinging() {
        // Given internet instance
        enableMobileData()

        // When
        collectInternetAvailability()
        awaitPing()

        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())

        // When
        awaitPing()

        // Then
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        assertEquals(listOf(true), collectedInternetAvailabilities)
    }

    @Test
    fun given_wifi_when_availability_collected_and_internet_disabled_internet_unavailable_and_pinging() {
        // Given internet instance
        enableWifi()

        // When
        collectInternetAvailability()
        awaitPing()
        disableInternet()
        awaitPing()

        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isNotEmpty())
        assert(!internet.availability.value)
        assertEquals(listOf(true, false), collectedInternetAvailabilities)
    }

    @Test
    fun given_mobile_data_when_availability_collected_and_internet_disabled_then_internet_available_and_no_pinging() {
        // Given internet instance
        enableMobileData()

        // When
        collectInternetAvailability()
        awaitPing()
        disableInternet()
        awaitPing()

        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        assert(internet.pings.isEmpty())
        assert(internet.availability.value)
        assertEquals(listOf(true), collectedInternetAvailabilities)
    }

    @Test
    fun given_wifi_when_internet_toggled_then_correctly_emitted_availability_and_pinging() {
        // Given internet instance
        enableWifi()

        // When
        collectInternetAvailability()
        awaitPing()
        disableInternet()
        awaitPing()
        // Then
        assert(collectedInternetAvailabilities.isNotEmpty())
        val count = internet.pings.size
        assert(internet.pings.isNotEmpty())
        assert(!internet.availability.value)

        // When
        enableInternet()
        awaitPing()
        // Then
        assert(internet.availability.value)

        // When
        disableInternet()
        awaitPing()
        // Then
        assert(!internet.availability.value)

        // When
        enableInternet()
        awaitPing()
        // Then
        assert(internet.pings.size > count)
        assert(internet.availability.value)
        assertEquals(listOf(true, false, true, false, true), collectedInternetAvailabilities)
    }
}