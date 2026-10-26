package com.metrolist.linux

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.metrolist.linux.ui.MetrolistApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Metrolist Linux",
    ) {
        MetrolistApp()
    }
}
