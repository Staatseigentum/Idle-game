package com.embercrown.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.embercrown.game.reboot.AshPalette
import com.embercrown.game.ui.pixelFontFamily
import java.awt.Cursor
import java.awt.Point
import java.awt.Toolkit
import java.awt.image.BufferedImage

/** Drawn in code so the cursor stays crisp without platform-specific image resources. */
internal fun emberCursor(flameLook: String = "ember"): Cursor = runCatching {
    val toolkit = Toolkit.getDefaultToolkit()
    val supported = toolkit.getBestCursorSize(32, 32)
    if (supported.width <= 0 || supported.height <= 0) return@runCatching Cursor.getDefaultCursor()
    val pixels = listOf(
        "X...............", "XX..............", "XEX.............", "XHEX............",
        "XHHEX...........", "XHHHEX..........", "XHHHHEX.........", "XHHHHHEX........",
        "XHHHHHHEX.......", "XHHHHHHHEX......", "XHHHXXXXXX......", "XHHEX...........",
        "XHEXEX..........", "XX.XEX..........", "X...XEX.........", ".....XX.........",
    )
    val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
    val (light, core) = when (flameLook) {
        "moonfire" -> 0xFFB8F4F6.toInt() to 0xFF76D8E8.toInt()
        "witchfire" -> 0xFFE7BEFF.toInt() to 0xFFB77DE3.toInt()
        "ghostfire" -> 0xFFE5F8D7.toInt() to 0xFFA8D8B3.toInt()
        else -> 0xFFFFD784.toInt() to 0xFFFF8D42.toInt()
    }
    val colors = mapOf('X' to 0xFF18131D.toInt(), 'H' to light, 'E' to core)
    pixels.forEachIndexed { y, row ->
        row.forEachIndexed { x, pixel ->
            val color = colors[pixel]
            if (color != null) for (dy in 0..1) for (dx in 0..1) {
                image.setRGB(x * 2 + dx, y * 2 + dy, color)
            }
        }
    }
    toolkit.createCustomCursor(image, Point(0, 0), "Embercrown ember")
}.getOrElse { Cursor.getDefaultCursor() }

@Composable
internal fun WindowScope.EmberTitleBar(state: WindowState, icon: Painter, onClose: () -> Unit) {
    val font = pixelFontFamily()
    WindowDraggableArea(modifier = Modifier.fillMaxWidth().height(38.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(38.dp)
                .background(AshPalette.night)
                .border(width = 1.dp, color = AshPalette.edge.copy(alpha = 0.7f))
                .padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(painter = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("EMBERCROWN", color = AshPalette.flameLight,
                fontFamily = font, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${BuildInfo.VERSION}  /  THE BLACK COURT", color = AshPalette.muted,
                fontFamily = font, fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            WindowControl("_", "Minimize window", AshPalette.bone) { state.isMinimized = true }
            WindowControl(if (state.placement == WindowPlacement.Maximized) "[]" else "[ ]",
                "Maximize or restore window", AshPalette.bone) {
                state.placement = if (state.placement == WindowPlacement.Maximized)
                    WindowPlacement.Floating else WindowPlacement.Maximized
            }
            WindowControl("X", "Close window", AshPalette.crimson, onClose)
        }
    }
}

@Composable
private fun WindowControl(label: String, description: String, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = Modifier.width(42.dp).fillMaxHeight()
            .background(if (hovered) accent.copy(alpha = 0.25f) else Color.Transparent)
            .hoverable(interaction)
            .semantics { contentDescription = description; role = Role.Button }
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (hovered) AshPalette.flameLight else AshPalette.bone,
            fontFamily = pixelFontFamily(), fontSize = 10.sp)
    }
}
