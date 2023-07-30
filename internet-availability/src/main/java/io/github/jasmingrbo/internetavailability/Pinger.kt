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

import android.net.Network
import android.util.Patterns
import io.github.jasmingrbo.internetavailability.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.lang.System.getProperty
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.Proxy
import java.net.Socket
import java.net.URL
import kotlin.coroutines.coroutineContext

internal class Pinger(
    url: String,
    private val timeoutMillis: Int,
    private val retryTimes: Int,
    private val retryIntervalMillis: Long
) {
    init {
        require(Patterns.WEB_URL.matcher(url).matches()) { "Invalid url: $url" }
    }

    private val url = try {
        URL(url)
    } catch (e: MalformedURLException) {
        throw IllegalArgumentException(e.message)
    }

    private var hostAddress: String = ""
        get() = field.ifEmpty {
            InetAddress.getByName(this.url.host).hostAddress?.also { hostAddress ->
                this.hostAddress = hostAddress
            } ?: throw IllegalArgumentException("Unknown host: ${url.host}")
        }

    internal suspend fun ping(
        network: Network,
        cancellationValue: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!BuildConfig.DEBUG) {
                network.createSocket()
            } else {
                network.createSocket(
                    host = getProperty("http.proxyHost"),
                    port = getProperty("http.proxyPort")
                )
            }.run {
                retry(retryTimes, retryIntervalMillis, cancellationValue) { attempt ->
                    connect(attempt)
                }
            }
        } catch (e: Exception) {
            Logger.e(e)
            false
        }.also { connected ->
            Logger.d("Pinged ${if (connected) "successfully" else "unsuccessfully"}")
        }
    }

    private suspend fun retry(
        times: Int,
        delayTimeMillis: Long,
        cancellationValue: Boolean,
        action: suspend (Int) -> Boolean
    ): Boolean {
        var count = -1
        var result = false

        while (coroutineContext.isActive && ++count <= times && !result) {
            result = action(count)
            delay(delayTimeMillis)
        }

        return if (!coroutineContext.isActive) cancellationValue || result else result
    }

    private fun Network.createSocket(
        host: String? = null,
        port: String? = null
    ): Socket = if (host == null || port == null || "$host:$port" != Internet.TEST_PROXY) {
        socketFactory.createSocket()
    } else {
        Socket(Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port.toInt()))).also { socket ->
            bindSocket(socket)
            Logger.d("Using proxy $host:$port to ping.")
        }
    }

    private fun Socket.connect(attempt: Int): Boolean = try {
        Logger.d("attempt: $attempt - About to connect to: $hostAddress:${url.defaultPort}")
        connect(InetSocketAddress(hostAddress, url.defaultPort), timeoutMillis)
        close()
        true
    } catch (e: Exception) {
        Logger.e(e)
        false
    }
}