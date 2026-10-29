package com.metrolist.linux.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * The app icon (a purple disc with a play triangle), drawn in code so no image asset is needed.
 * Used for the window and the system tray.
 */
object AppIcon : Painter() {
    override val intrinsicSize: Size = Size(64f, 64f)

    override fun DrawScope.onDraw() {
        drawCircle(color = Color(0xFF6750A4))

        val w = size.width
        val h = size.height
        val triangle = Path().apply {
            moveTo(w * 0.40f, h * 0.28f)
            lineTo(w * 0.40f, h * 0.72f)
            lineTo(w * 0.74f, h * 0.50f)
            close()
        }
        drawPath(triangle, color = Color.White)
    }
}
