package com.embercrown.game.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** One-shot notifications for feedback (sound/animation) that a [StateFlow] snapshot can't carry. */
sealed interface GameEvent {
    data class GoldTapped(val amount: Double) : GameEvent
    data class PurchaseSucceeded(val buildingId: String) : GameEvent
    data class PurchaseDenied(val buildingId: String) : GameEvent
    data object AcademyStudySucceeded : GameEvent
    data object AcademyStudyDenied : GameEvent
    data class AchievementUnlocked(val id: String) : GameEvent
    data class AgeAdvanced(val newIndex: Int) : GameEvent
}

class GameEngine(
    private val scope: CoroutineScope,
    private val repository: SaveRepository,
) {
    private val _state = MutableStateFlow(repository.load())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    init {
        applyOfflineProgress()
        scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                tick(TICK_INTERVAL_MS / 1000.0)
            }
        }
        scope.launch {
            while (isActive) {
                delay(AUTOSAVE_INTERVAL_MS)
                persistNow()
            }
        }
    }

    fun tick(deltaSeconds: Double) = mutate { s ->
        val gained = totalProduction(s) * deltaSeconds
        s.copy(gold = s.gold + gained, lifetimeGold = s.lifetimeGold + gained)
    }

    fun click() {
        var gained = 0.0
        mutate { s ->
            gained = clickGain(s)
            s.copy(gold = s.gold + gained, lifetimeGold = s.lifetimeGold + gained)
        }
        _events.tryEmit(GameEvent.GoldTapped(gained))
    }

    /** Buys [quantity] levels of [buildingId], or as many as affordable if [quantity] is null (MAX). */
    fun buy(buildingId: String, quantity: Int? = null) {
        var succeeded = false
        mutate { s ->
            val definition = BuildingDefinition.byId(buildingId)
            if (!isBuildingUnlocked(definition, s)) return@mutate s
            val level = s.buildingLevel(buildingId)
            val qty = quantity ?: maxAffordableQuantity(definition, level, s.gold)
            if (qty <= 0) return@mutate s
            val cost = bulkBuildingCost(definition, level, qty)
            if (s.gold < cost) return@mutate s
            succeeded = true
            s.copy(
                gold = s.gold - cost,
                buildings = s.buildings.map {
                    if (it.id == buildingId) it.copy(level = it.level + qty) else it
                },
            )
        }
        _events.tryEmit(if (succeeded) GameEvent.PurchaseSucceeded(buildingId) else GameEvent.PurchaseDenied(buildingId))
    }

    fun canTriggerVerfall(): Boolean = _state.value.lifetimeGold >= MIN_LIFETIME_GOLD_FOR_VERFALL

    /** Resets the current life (gold, lifetime gold, buildings) in exchange for permanent Chronicle Points. */
    fun triggerVerfall() {
        mutate { s ->
            if (s.lifetimeGold < MIN_LIFETIME_GOLD_FOR_VERFALL) return@mutate s
            val gained = chroniclePointsForVerfall(s)
            GameState(
                chroniclePoints = s.chroniclePoints + gained,
                lifetimeChroniclePoints = s.lifetimeChroniclePoints + gained,
                academyLevel = s.academyLevel,
                verfallCount = s.verfallCount + 1,
                sagen = s.sagen,
                wiedergeburtCount = s.wiedergeburtCount,
                activeDynastyPath = s.activeDynastyPath,
                legacyLevels = s.legacyLevels,
                unlockedAchievements = s.unlockedAchievements,
                offlineProgressEnabled = s.offlineProgressEnabled,
                lastSeenEpochSeconds = nowEpochSeconds(),
            )
        }
        persistNow()
    }

    fun buyAcademyUpgrade() {
        var succeeded = false
        mutate { s ->
            val cost = academyUpgradeCost(s.academyLevel)
            if (s.chroniclePoints < cost) return@mutate s
            succeeded = true
            s.copy(chroniclePoints = s.chroniclePoints - cost, academyLevel = s.academyLevel + 1)
        }
        _events.tryEmit(if (succeeded) GameEvent.AcademyStudySucceeded else GameEvent.AcademyStudyDenied)
    }

    fun canTriggerWiedergeburt(): Boolean = _state.value.verfallCount >= MIN_VERFALL_COUNT_FOR_WIEDERGEBURT

    /** Deep reset: wipes Chronicle Points, Academy and Age progress in exchange for permanent Legends. */
    fun triggerWiedergeburt() {
        mutate { s ->
            if (s.verfallCount < MIN_VERFALL_COUNT_FOR_WIEDERGEBURT) return@mutate s
            val gained = sagenForWiedergeburt(s.lifetimeChroniclePoints)
            GameState(
                sagen = s.sagen + gained,
                wiedergeburtCount = s.wiedergeburtCount + 1,
                activeDynastyPath = s.activeDynastyPath,
                legacyLevels = s.legacyLevels,
                unlockedAchievements = s.unlockedAchievements,
                offlineProgressEnabled = s.offlineProgressEnabled,
                lastSeenEpochSeconds = nowEpochSeconds(),
            )
        }
        persistNow()
    }

    fun buyLegacyUpgrade(path: DynastyPath) = mutate { s ->
        val level = s.legacyLevels[path] ?: 0
        val cost = legacyUpgradeCost(level)
        if (s.sagen < cost) return@mutate s
        s.copy(sagen = s.sagen - cost, legacyLevels = s.legacyLevels + (path to level + 1))
    }

    fun setActiveDynastyPath(path: DynastyPath) = mutate { s -> s.copy(activeDynastyPath = path) }

    fun setOfflineProgressEnabled(enabled: Boolean) = mutate { s -> s.copy(offlineProgressEnabled = enabled) }

    /** Wipes ALL progress, including Chronicle Points, Academy and Achievements. */
    fun hardReset() {
        _state.update { GameState() }
        persistNow()
    }

    fun exportSave(): String = repository.serialize(_state.value.copy(lastSeenEpochSeconds = nowEpochSeconds()))

    /** Replaces the current state with [raw] JSON if it parses successfully. Returns true on success. */
    fun importSave(raw: String): Boolean {
        val parsed = repository.deserialize(raw) ?: return false
        _state.update { parsed }
        persistNow()
        return true
    }

    fun persistNow() {
        repository.save(_state.value.copy(lastSeenEpochSeconds = nowEpochSeconds()))
    }

    private fun mutate(block: (GameState) -> GameState) {
        lateinit var prev: GameState
        lateinit var next: GameState
        _state.update { s ->
            prev = s
            next = refreshAchievements(block(s))
            next
        }
        (next.unlockedAchievements - prev.unlockedAchievements).forEach {
            _events.tryEmit(GameEvent.AchievementUnlocked(it))
        }
        // `>` not `!=`: triggerVerfall/triggerWiedergeburt rebuild a fresh GameState that resets
        // lifetimeGold (and therefore currentAge back down) — that must not fire a fanfare.
        if (next.currentAge.index > prev.currentAge.index) {
            _events.tryEmit(GameEvent.AgeAdvanced(next.currentAge.index))
        }
    }

    private fun refreshAchievements(s: GameState): GameState {
        val newlyUnlocked = AchievementDefinition.all
            .asSequence()
            .filter { it.id !in s.unlockedAchievements && it.condition(s) }
            .map { it.id }
            .toList()
        return if (newlyUnlocked.isEmpty()) s else s.copy(unlockedAchievements = s.unlockedAchievements + newlyUnlocked)
    }

    private fun applyOfflineProgress() {
        val now = nowEpochSeconds()
        val last = _state.value.lastSeenEpochSeconds
        if (last > 0L && _state.value.offlineProgressEnabled) {
            val elapsedSeconds = (now - last).coerceIn(0L, MAX_OFFLINE_SECONDS)
            if (elapsedSeconds > 0L) {
                tick(elapsedSeconds.toDouble())
            }
        }
        _state.update { it.copy(lastSeenEpochSeconds = now) }
    }

    @OptIn(ExperimentalTime::class)
    private fun nowEpochSeconds(): Long = Clock.System.now().toEpochMilliseconds() / 1000L

    companion object {
        private const val TICK_INTERVAL_MS = 1000L
        private const val AUTOSAVE_INTERVAL_MS = 10_000L
        private const val MAX_OFFLINE_SECONDS = 8L * 3600L
    }
}
