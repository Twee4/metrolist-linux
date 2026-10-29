package com.metrolist.linux

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.metrolist.linux.ui.MetrolistApp
import java.awt.Dimension

// Compose's default is 800x600, which is too cramped for the player bar. This opens comfortably
// on a 1080p screen, and the minimum keeps the layout from collapsing if the user shrinks it.
private val INITIAL_SIZE = DpSize(2880.dp, 1800.dp)
private const val MIN_WIDTH = 960
private const val MIN_HEIGHT = 600

fun main() = application {
    val windowState = rememberWindowState(
        size = INITIAL_SIZE,
        position = WindowPosition.Aligned(Alignment.Center),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Metrolist Linux",
        state = windowState,
    ) {
        LaunchedEffect(window) { window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT) }
        MetrolistApp()
    }
}
