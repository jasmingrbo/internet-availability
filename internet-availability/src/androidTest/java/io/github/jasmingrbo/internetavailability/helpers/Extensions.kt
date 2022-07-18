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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal fun <T> Flow<T>.collectLatestWithDelayOnFirstCollection(
    collector: MutableList<T>,
    scope: CoroutineScope
) = scope.launch {
    var firstCollection = true
    collectLatest { element ->
        // Adjust time to populate networks
        if (firstCollection) {
            firstCollection = false
            delay(250)
        }
        collector.add(element)
    }
}


internal fun <T> Flow<T>.collectLatest(
    collector: MutableList<T>,
    scope: CoroutineScope
) = scope.launch { collectLatest { element -> collector.add(element) } }