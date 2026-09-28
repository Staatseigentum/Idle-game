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
    val musicVolume: Float = 0.5f,
    val effectsVolume: Float = 0.75f,
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

    fun setMusicVolume(volume: Float) {
        val safe = volume.coerceIn(0f, 1f)
        _state.update { it.copy(musicVolume = safe) }
        settings[KEY_MUSIC_VOLUME] = safe
    }

    fun setEffectsVolume(volume: Float) {
        val safe = volume.coerceIn(0f, 1f)
        _state.update { it.copy(effectsVolume = safe) }
        settings[KEY_EFFECTS_VOLUME] = safe
    }

    private fun load(): UiSettings = UiSettings(
        language = settings.getStringOrNull(KEY_LANGUAGE),
        masterVolume = settings[KEY_VOLUME, 0.7f],
        muted = settings[KEY_MUTED, false],
        musicVolume = settings[KEY_MUSIC_VOLUME, 0.5f],
        effectsVolume = settings[KEY_EFFECTS_VOLUME, 0.75f],
    )

    companion object {
        private const val KEY_LANGUAGE = "embercrown_language_v1"
        private const val KEY_VOLUME = "embercrown_master_volume_v1"
        private const val KEY_MUTED = "embercrown_muted_v1"
        private const val KEY_MUSIC_VOLUME = "embercrown_music_volume_v1"
        private const val KEY_EFFECTS_VOLUME = "embercrown_effects_volume_v1"
    }
}
