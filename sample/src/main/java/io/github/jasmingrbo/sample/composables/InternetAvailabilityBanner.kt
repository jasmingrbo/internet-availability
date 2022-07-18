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

package io.github.jasmingrbo.sample.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jasmingrbo.sample.R

@Composable
internal fun AnimatedInternetAvailabilityBanner(hasInternet: Boolean) {
    AnimatedVisibility(
        visible = !hasInternet,
        enter = expandVertically(animationSpec = tween(easing = LinearOutSlowInEasing)),
        exit = shrinkVertically(
            animationSpec = tween(
                durationMillis = 200,
                delayMillis = if (hasInternet) 3000 else 0,
                easing = FastOutLinearInEasing
            )
        )
    ) { InternetAvailabilityBanner(hasInternet = hasInternet) }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun InternetAvailabilityBanner(
    modifier: Modifier = Modifier,
    hasInternet: Boolean
) {
    val transition = updateTransition(
        targetState = hasInternet,
        label = "InternetAvailabilityBanner"
    )
    val surfaceColor by transition.animateColor(label = "surfaceColor") { targetHasInternet ->
        if (targetHasInternet) MaterialTheme.colors.secondary
        else MaterialTheme.colors.error
    }
    Surface(
        modifier = modifier,
        shape = RectangleShape,
        color = surfaceColor,
    ) {
        transition.AnimatedContent { internet ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(
                        if (internet) R.drawable.ic_wifi_on else R.drawable.ic_wifi_off
                    ),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (internet) R.string.internet_available else R.string.internet_unavailable
                    ),
                    style = MaterialTheme.typography.body2.copy(fontSize = 12.sp)
                )
            }
        }
    }
}