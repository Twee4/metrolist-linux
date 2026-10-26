package com.metrolist.linux

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Metrolist Linux", style = MaterialTheme.typography.headlineMedium)
                Text("Desktop starter app", style = MaterialTheme.typography.bodyLarge)
                Text("This project is a native Linux desktop foundation for the Metrolist ecosystem.")
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Metrolist Linux"
    ) {
        App()
    }
}
