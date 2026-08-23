package com.embercrown.game.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * Runtime override for the locale Compose Resources resolves strings against.
 * `provides(null)` restores whatever the OS default was before any override was applied.
 */
expect object LocalAppLocale {
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
