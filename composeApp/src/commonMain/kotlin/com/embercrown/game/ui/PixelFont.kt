package com.embercrown.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import com.embercrown.game.resources.Res
import com.embercrown.game.resources.press_start_2p
import org.jetbrains.compose.resources.Font

@Composable
fun pixelFontFamily(): FontFamily {
    val font = Font(Res.font.press_start_2p)
    return remember(font) { FontFamily(font) }
}
