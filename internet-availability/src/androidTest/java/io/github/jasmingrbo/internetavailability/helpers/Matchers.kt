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

import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

internal fun <T> equalsOneOf(vararg elements: T) = object : TypeSafeMatcher<T>() {
    override fun describeTo(description: Description) {
        for ((index, element) in elements.withIndex()) {
            description.appendText("<$element>")
            if (index != elements.lastIndex) description.appendText(" or ")
        }
    }

    override fun matchesSafely(item: T) = elements.contains(item)
}