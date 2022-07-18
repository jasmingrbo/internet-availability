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

package io.github.jasmingrbo.internetavailability.logging

import io.github.jasmingrbo.internetavailability.BuildConfig
import io.github.jasmingrbo.internetavailability.logging.Tree.Companion.DEBUG_TREE
import io.github.jasmingrbo.internetavailability.logging.Tree.Companion.RELEASE_TREE
import timber.log.Timber

internal object Logger {
    init {
        Timber.plant(if (BuildConfig.DEBUG) Timber.DEBUG_TREE else Timber.RELEASE_TREE)
    }

    internal fun d(message: String) {
        Timber.d(message)
    }

    internal fun e(throwable: Throwable) {
        Timber.e(throwable)
    }
}