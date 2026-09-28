package com.embercrown.game.game

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun createSettings(): Settings {
    val profile = System.getenv("EMBERCROWN_PROFILE")
    require(profile == null || Regex("[A-Za-z0-9_-]{1,48}").matches(profile)) {
        "EMBERCROWN_PROFILE must be a short alphanumeric profile name"
    }
    val node = if (profile == null) "com/embercrown/game" else "com/embercrown/game/profiles/$profile"
    return PreferencesSettings(Preferences.userRoot().node(node))
}
