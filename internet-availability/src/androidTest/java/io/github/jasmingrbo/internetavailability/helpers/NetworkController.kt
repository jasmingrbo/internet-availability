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

package io.github.jasmingrbo.internetavailability.helpers

import androidx.test.platform.app.InstrumentationRegistry
import io.github.jasmingrbo.internetavailability.Internet
import io.github.jasmingrbo.internetavailability.helpers.Awaiter.await

internal object NetworkController {
    private const val wifiCommand = "svc wifi"
    private const val mobileDataCommand = "svc data"
    private const val proxyCommand = "settings put global http_proxy"

    private val executeShellCommand =
        InstrumentationRegistry.getInstrumentation().uiAutomation::executeShellCommand

    internal fun enableWifi() {
        setWifi(true)
    }

    internal fun disableWifi() {
        setWifi(false)
    }

    internal fun enableMobileData() {
        setMobileData(true)
    }

    internal fun disableMobileData() {
        setMobileData(false)
    }

    internal fun enableNetworks(wifiFirst: Boolean = true) {
        setNetworks(true, wifiFirst)
    }

    internal fun disableNetworks(wifiFirst: Boolean = true) {
        setNetworks(false, wifiFirst)
    }

    internal fun enableInternet() {
        setInternet(true)
    }

    internal fun disableInternet() {
        setInternet(false)
    }

    private fun setWifi(enable: Boolean) {
        executeShellCommand(wifiCommand.completeNetworkCommand(enable))
        awaitNetwork(enable)
    }

    private fun setMobileData(enable: Boolean) {
        executeShellCommand(mobileDataCommand.completeNetworkCommand(enable))
        awaitNetwork(enable)
    }

    private fun setNetworks(enable: Boolean, wifiFirst: Boolean) {
        if (wifiFirst) {
            setWifi(enable)
            setMobileData(enable)
        } else {
            setMobileData(enable)
            setWifi(enable)
        }
    }

    private fun setInternet(enable: Boolean) {
        executeShellCommand(proxyCommand.completeProxyCommand(enable))
        await(1_000)
    }

    private fun awaitNetwork(enable: Boolean) {
        await(if (enable) 5_000 else 2_000)
    }

    private fun String.completeNetworkCommand(enable: Boolean): String {
        return completeCommand(enable, "enable", "disable")
    }

    private fun String.completeProxyCommand(enable: Boolean): String {
        return completeCommand(enable, ":0", Internet.TEST_PROXY)
    }

    private fun String.completeCommand(
        enable: Boolean,
        enabled: String,
        disabled: String
    ) = "$this ${if (enable) enabled else disabled}"
}