package com.embercrown.game.game

import com.embercrown.game.audio.MusicBank
import com.embercrown.game.audio.MusicPlayer
import com.embercrown.game.audio.SoundBank
import com.embercrown.game.audio.SoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AppGraph {
    private val settings by lazy { createSettings() }

    val engine: GameEngine by lazy {
        GameEngine(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            repository = SaveRepository(settings),
        )
    }

    val uiSettings: UiSettingsStore by lazy { UiSettingsStore(settings) }

    val soundPlayer: SoundPlayer by lazy {
        SoundPlayer().apply {
            preload(SoundBank.clips)
            val initial = uiSettings.state.value
            setVolume(initial.masterVolume)
            setMuted(initial.muted)
        }
    }

    val musicPlayer: MusicPlayer by lazy {
        MusicPlayer().apply {
            val initial = uiSettings.state.value
            setVolume(initial.masterVolume)
            setMuted(initial.muted)
            preload(MusicBank.theme())
        }
    }
}
