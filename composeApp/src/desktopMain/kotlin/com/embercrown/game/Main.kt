package com.embercrown.game

import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.embercrown.game.ui.App
import com.embercrown.game.ui.pixelart.appIcon
import com.embercrown.game.ui.pixelart.toImageBitmap

fun main() = application {
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        width = 1100.dp,
        height = 780.dp,
    )
    val windowIcon = remember { BitmapPainter(appIcon().toImageBitmap()) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Embercrown",
        state = windowState,
        icon = windowIcon,
    ) {
        window.minimumSize = java.awt.Dimension(480, 720)
        App()
    }
}
