package com.metrolist.linux

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import androidx.compose.ui.window.rememberWindowState
import com.metrolist.linux.ui.AppIcon
import com.metrolist.linux.ui.MetrolistApp
import com.metrolist.shared.player.MprisService
import com.metrolist.shared.player.MpvAudioBackend
import com.metrolist.shared.player.PlayerController
import java.awt.Dimension

// Initial window size; the minimum keeps the layout from collapsing if the user shrinks it.
private val INITIAL_SIZE = DpSize(2880.dp, 1800.dp)
private const val MIN_WIDTH = 960
private const val MIN_HEIGHT = 600

fun main() = application {
    // The player lives here, above the window, so the tray menu and media keys keep working
    // while the window is hidden.
    val player = remember { PlayerController(MpvAudioBackend()) }
    val playerState by player.state.collectAsState()
    val mpris = remember { MprisService(player) }

    LaunchedEffect(player) { player.start(this) }
    // Media keys and the desktop "now playing" widget. Does nothing if there is no D-Bus session.
    LaunchedEffect(mpris) { mpris.serve() }
    DisposableEffect(player) { onDispose { player.close() } }

    val windowState = rememberWindowState(
        size = INITIAL_SIZE,
        position = WindowPosition.Aligned(Alignment.Center),
    )
    var windowVisible by remember { mutableStateOf(true) }

    fun showWindow() {
        windowState.isMinimized = false
        windowVisible = true
    }

    // Not every desktop has a tray (GNOME needs the AppIndicator extension). Without one, closing the
    // window must quit, otherwise the app would be left running with no way to bring it back.
    if (isTraySupported) {
        Tray(
            icon = AppIcon,
            tooltip = playerState.current?.let { "${it.title} - ${it.artist}" } ?: "Metrolist Linux",
            onAction = { showWindow() },
            menu = {
                Item(playerState.current?.title ?: "Nothing playing", enabled = false, onClick = {})
                Separator()
                Item(
                    if (playerState.isPlaying) "Pause" else "Play",
                    enabled = playerState.current != null,
                    onClick = { player.togglePlayPause() },
                )
                Item("Next", enabled = playerState.hasNext, onClick = { player.next() })
                Item("Previous", enabled = playerState.current != null, onClick = { player.previous() })
                Separator()
                Item(
                    if (windowVisible) "Hide window" else "Show window",
                    onClick = { if (windowVisible) windowVisible = false else showWindow() },
                )
                Item("Quit", onClick = { exitApplication() })
            },
        )
    }

    Window(
        onCloseRequest = { if (isTraySupported) windowVisible = false else exitApplication() },
        visible = windowVisible,
        title = "Metrolist Linux",
        icon = AppIcon,
        state = windowState,
    ) {
        LaunchedEffect(window) { window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT) }
        MetrolistApp(player)
    }
}
