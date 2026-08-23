package com.embercrown.game.game

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Wall-clock epoch seconds — shared by [GameEngine] (offline-progress math) and the UI (cooldown countdowns). */
@OptIn(ExperimentalTime::class)
fun nowEpochSeconds(): Long = Clock.System.now().toEpochMilliseconds() / 1000L
