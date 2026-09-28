package com.embercrown.game.reboot

import com.embercrown.game.audio.AshAudioBank
import com.embercrown.game.audio.MusicPlayer
import com.embercrown.game.audio.SfxId
import com.embercrown.game.audio.SoundPlayer
import com.embercrown.game.game.UiSettings
import kotlin.time.TimeSource

/** One audio session shared by the desktop and mobile Embercrown entry points. */
class RebootAudio {
    private val effects = SoundPlayer()
    private val music = MusicPlayer()
    private var started = false
    private var lastTap = TimeSource.Monotonic.markNow()

    init {
        // No audio device is a valid state (headless Linux, unplugged headphones, etc.).
        runCatching { effects.preload(AshAudioBank.effects) }
        runCatching { music.preload(AshAudioBank.ambience) }
    }

    fun applySettings(settings: UiSettings) {
        runCatching {
            effects.setVolume(settings.masterVolume * settings.effectsVolume)
            effects.setMuted(settings.muted || settings.effectsVolume == 0f)
            music.setVolume(settings.masterVolume * settings.musicVolume * 0.7f)
            music.setMuted(settings.muted || settings.musicVolume == 0f)
        }
    }

    fun start() {
        if (started) return
        started = true
        runCatching { music.play() }
    }

    fun pause() {
        started = false
        runCatching { music.stop() }
    }

    fun play(id: SfxId) {
        if (id == SfxId.ASH_TAP) {
            if (lastTap.elapsedNow().inWholeMilliseconds < 85) return
            lastTap = TimeSource.Monotonic.markNow()
        }
        runCatching { effects.play(id) }
    }
}
