package com.embercrown.game.game

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiSettings(
    val language: String? = null,
    val masterVolume: Float = 0.7f,
    val muted: Boolean = false,
)

/**
 * Device-local UI preferences (language, volume, mute) — deliberately kept separate from
 * [GameState]/[SaveRepository] so they never round-trip through save export/import.
 */
class UiSettingsStore(private val settings: Settings) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<UiSettings> = _state.asStateFlow()

    fun setLanguage(code: String?) {
        _state.update { it.copy(language = code) }
        if (code == null) settings.remove(KEY_LANGUAGE) else settings[KEY_LANGUAGE] = code
    }

    fun setMasterVolume(volume: Float) {
        _state.update { it.copy(masterVolume = volume) }
        settings[KEY_VOLUME] = volume
    }

    fun setMuted(muted: Boolean) {
        _state.update { it.copy(muted = muted) }
        settings[KEY_MUTED] = muted
    }

    private fun load(): UiSettings = UiSettings(
        language = settings.getStringOrNull(KEY_LANGUAGE),
        masterVolume = settings[KEY_VOLUME, 0.7f],
        muted = settings[KEY_MUTED, false],
    )

    companion object {
        private const val KEY_LANGUAGE = "embercrown_language_v1"
        private const val KEY_VOLUME = "embercrown_master_volume_v1"
        private const val KEY_MUTED = "embercrown_muted_v1"
    }
}
