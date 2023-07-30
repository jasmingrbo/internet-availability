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

package io.github.jasmingrbo.sample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.jasmingrbo.sample.composables.SampleScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
internal class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: SampleViewModel by viewModels()
            with(viewModel) {
                // Doesn't work as it just suspends and not cancels, causing the upstream
                // not to drop the collector, hence continuing to produce values even when
                // the UI is in the background.
                // val hasInternet by viewModel.internetAvailability.collectAsState()

                val hasInternet by internetAvailability.collectAsStateWhenStarted()
                SampleScreen(
                    hasInternet = hasInternet,
                    text = textFieldValue,
                    onTextChange = ::onTextFieldValueChange
                )
            }
        }
    }

    /**
     * Collects values from this [StateFlow] when this [Composable]'s [LifecycleOwner]'s [Lifecycle]
     * is at least at [Lifecycle.State.STARTED] and represents its latest value via [State]. The
     * collection will be cancelled when the ON_STOP event happens and will restart if the
     * [Lifecycle] receives the ON_START event again. The [StateFlow.value] is used as an initial
     * value. Every time there would be new value posted into the [StateFlow] the returned [State]
     * will be updated causing recomposition of every [State.value] usage.
     */
    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    internal fun <T> StateFlow<T>.collectAsStateWhenStarted(): State<T> {
        val lifecycleOwner = LocalLifecycleOwner.current
        return produceState(value, this, lifecycleOwner) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { element -> value = element }
            }
        }
    }

    /**
     * Launches the collection of the given flow in the [scope]. Values from the flow are collected
     * when [LifecycleOwner]'s [Lifecycle] is at least at [Lifecycle.State.STARTED]. The collection
     * will be cancelled when the ON_STOP event happens and will restart if the [Lifecycle] receives
     * the ON_START event again.
     *
     * @param scope [CoroutineScope] in which the flow collection should happen.
     * @param onEach Callback to be invoked on each collected value.
     *
     * @return A reference to the coroutine's [Job] in which the flow collection is happening.
     */
    @Suppress("unused")
    internal fun <T> StateFlow<T>.collectWhenStarted(
        scope: CoroutineScope,
        onEach: (T) -> Unit
    ): Job = flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .onEach(onEach)
        .launchIn(scope)
}