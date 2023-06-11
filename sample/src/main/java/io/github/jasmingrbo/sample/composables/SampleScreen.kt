package io.github.jasmingrbo.sample.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

@Composable
fun SampleScreen(
    modifier: Modifier = Modifier,
    hasInternet: Boolean,
    text: String,
    onTextChange: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        AnimatedInternetAvailabilityBanner(hasInternet = hasInternet)
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(text = "Anything...") }
        )
    }
}

@Preview
@Composable
fun PreviewSampleScreen() {
    val (text, onTextChange) = remember { mutableStateOf("") }
    SampleScreen(hasInternet = true, text = text, onTextChange = onTextChange)
}

@Preview
@Composable
fun PreviewSampleScreenNoInternet() {
    val (text, onTextChange) = remember { mutableStateOf("") }
    SampleScreen(hasInternet = false, text = text, onTextChange = onTextChange)
}

@Preview
@Composable
fun PreviewSampleScreenToggleInternet() {
    val (text, onTextChange) = remember { mutableStateOf("") }
    var hasInternet by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = hasInternet) {
        delay(1_000)
        hasInternet = !hasInternet
    }
    SampleScreen(hasInternet = hasInternet, text = text, onTextChange = onTextChange)
}