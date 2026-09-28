package com.embercrown.game

import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.embercrown.game.reboot.RebootApp
import com.embercrown.game.reboot.RebootGraph
import com.embercrown.game.reboot.cosmeticStyle
import com.embercrown.game.ui.pixelart.appIcon
import com.embercrown.game.ui.pixelart.toImageBitmap

fun main() = application {
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        width = 1280.dp,
        height = 800.dp,
    )
    val windowIcon = remember { BitmapPainter(appIcon().toImageBitmap()) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Embercrown",
        state = windowState,
        icon = windowIcon,
        undecorated = true,
    ) {
        window.minimumSize = java.awt.Dimension(480, 720)
        val gameState by RebootGraph.engine.state.collectAsState()
        val flameLook = cosmeticStyle(gameState, "flame")
        val cursor = remember(flameLook) { emberCursor(flameLook) }
        window.cursor = cursor
        val pointerIcon = remember(cursor) { PointerIcon(cursor) }
        Column(Modifier.fillMaxSize().pointerHoverIcon(pointerIcon)) {
            EmberTitleBar(windowState, windowIcon, ::exitApplication)
            Box(Modifier.weight(1f).fillMaxWidth()) { RebootApp() }
        }
    }
}
