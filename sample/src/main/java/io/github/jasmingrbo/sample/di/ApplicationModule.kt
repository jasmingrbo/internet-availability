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

package io.github.jasmingrbo.sample.di

import android.content.Context
import io.github.jasmingrbo.internetavailability.Internet
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object ApplicationModule {
    @DefaultDispatcher
    @Provides
    internal fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Singleton
    @ApplicationScope
    @Provides
    internal fun provideCoroutineScope(
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    @Singleton
    @InternetAvailability
    @Provides
    internal fun provideInternetAvailability(
        @ApplicationScope scope: CoroutineScope,
        @ApplicationContext context: Context
    ): StateFlow<Boolean> = Internet.getInstance(scope = scope, context = context).availability
}