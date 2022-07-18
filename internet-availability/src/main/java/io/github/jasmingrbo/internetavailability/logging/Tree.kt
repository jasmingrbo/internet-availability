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
import timber.log.Timber

internal abstract class Tree : Timber.DebugTree() {
    protected lateinit var element: StackTraceElement

    private val StackTraceElement.next: StackTraceElement
        get() {
            val stackTrace = Throwable().stackTrace
            val index = stackTrace.indexOf(this)
            return stackTrace[index + 1]
        }

    protected val StackTraceElement.info: String
        get() = String.format(
            "(%s:%s) - %s.%s",
            fileName,
            lineNumber,
            className.substring(className.lastIndexOf(".") + 1),
            methodName
        )

    override fun createStackElementTag(element: StackTraceElement): String? {
        this.element = element.next // Timber calls are wrapped inside of Logger
        return BuildConfig.LIBRARY_PACKAGE_NAME
    }

    internal companion object {
        val Timber.Forest.DEBUG_TREE: Tree
            get() = object : Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    super.log(priority, tag, createMessage(message, element), t)
                }

                private fun createMessage(
                    message: String,
                    element: StackTraceElement,
                ) = String.format("%s - %s", element.info, message)
            }

        val Timber.Forest.RELEASE_TREE: Tree
            get() = object : Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    return
                }

                override fun createStackElementTag(element: StackTraceElement): String? {
                    return null
                }
            }
    }
}