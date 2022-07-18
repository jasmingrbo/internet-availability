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

package io.github.jasmingrbo.internetavailability.common

import io.github.jasmingrbo.internetavailability.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry

internal fun <T> MutableSharedFlow<T>.collectSubscriptionCount(
    scope: CoroutineScope,
    onCountGreaterThanZero: () -> Unit,
    onCountZero: () -> Unit
) {
    var wasActive = false

    subscriptionCount
        .map { count -> count > 0 }
        .distinctUntilChanged()
        .onEach { isActive ->
            if (isActive) {
                onCountGreaterThanZero()
                wasActive = true
            } else if (wasActive) {
                onCountZero()
            }
        }
        .onCompletion { onCountZero() }
        .retry { throwable ->
            Logger.e(throwable)
            delay(1_000)
            true
        }
        .flowOn(Dispatchers.IO)
        .launchIn(scope)
}