package com.embercrown.game.ui.pixelart

import androidx.compose.ui.graphics.Color

/** Shared color palette for the Embercrown pixel-art theme. */
object EmberPalette {
    val Gold = Color(0xFFE0A94D)
    val GoldBright = Color(0xFFF4D28C)
    val GoldDim = Color(0xFF9C7A3E)
    val Background = Color(0xFF14100C)
    val Panel = Color(0xFF231B14)
    val PanelLight = Color(0xFF2E2318)

    // The redesigned layout's header bar / progress row / footer nav / inset cards, and the
    // ages rail + Reich column backgrounds — both slightly darker than Panel, distinct from it.
    val HeaderBar = Color(0xFF1B1510)
    val Rail = Color(0xFF181310)
    val Accent = Color(0xFF8B2E2E)
    val AccentBright = Color(0xFFB23A3A)
    val Dim = Color(0xFF6B5A46)
    val Shadow = Color(0xFF0A0806)

    // Pixel-bevel border tones (raised = light on top/left, dark on bottom/right).
    val BevelLight = Color(0xFF4A3A28)
    val BevelDark = Color(0xFF080604)

    val White = Color(0xFFF0E6D2)
    val Ink = Color(0xFF120D0A)

    // Legacy single tones still used by the age scene's flat areas.
    val Stone = Color(0xFF6E6E6E)
    val StoneDark = Color(0xFF48484A)
    val StoneLight = Color(0xFF9A9A9C)
    val Wood = Color(0xFF7A4A28)
    val WoodDark = Color(0xFF4A2C18)
    val Roof = Color(0xFF9C3B2E)
    val RoofDark = Color(0xFF6B2620)
    val Leaf = Color(0xFF3C5E3C)
    val Water = Color(0xFF3E7A8C)
    val WaterDark = Color(0xFF2A5866)
    val Flame = Color(0xFFE8792E)
    val FlameBright = Color(0xFFF4C24C)
}

/**
 * Dark→light endpoints for each material. Every sprite picks two or three of these and lets
 * [PixelGridBuilder.ditherRamp] blend the steps in between, which is what gives the art its
 * shaded, non-flat look.
 */
object Ramps {
    val StoneDark = Color(0xFF33333A)
    val StoneLight = Color(0xFFCFCFD6)

    val WoodDark = Color(0xFF2E1A0E)
    val WoodLight = Color(0xFFB98247)

    val GoldDark = Color(0xFF6B4310)
    val GoldLight = Color(0xFFFDECB4)

    val RoofDark = Color(0xFF4E1712)
    val RoofLight = Color(0xFFD9705A)

    val WaterDark = Color(0xFF12303E)
    val WaterLight = Color(0xFF8CD8ED)

    val LeafDark = Color(0xFF17331A)
    val LeafLight = Color(0xFF87C96C)

    val FlameDark = Color(0xFF7A2405)
    val FlameLight = Color(0xFFFFEDB0)

    val ArcaneDark = Color(0xFF261544)
    val ArcaneLight = Color(0xFFCBA8F2)

    val ClothDark = Color(0xFF5A1526)
    val ClothLight = Color(0xFFE0748A)

    val PaperDark = Color(0xFF8A7550)
    val PaperLight = Color(0xFFFBF3DC)

    val CorruptionDark = Color(0xFF120014)
    val CorruptionLight = Color(0xFF4A1D52)
}
