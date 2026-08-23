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
import kotlin.random.Random

/** One-shot notifications for feedback (sound/animation) that a [StateFlow] snapshot can't carry. */
sealed interface GameEvent {
    data class GoldTapped(val amount: Double) : GameEvent
    data class PurchaseSucceeded(val buildingId: String) : GameEvent
    data class PurchaseDenied(val buildingId: String) : GameEvent
    data class BuildingMilestoneReached(val buildingId: String, val level: Int) : GameEvent
    data object AcademyStudySucceeded : GameEvent
    data object AcademyStudyDenied : GameEvent
    data class AchievementUnlocked(val id: String) : GameEvent
    data class AgeAdvanced(val newIndex: Int) : GameEvent
    data object DragonBuffActivated : GameEvent
    data class RatssaalUpgradePurchased(val id: String) : GameEvent
    data class RatssaalUpgradeDenied(val id: String) : GameEvent
    data class QuestClaimed(val id: String) : GameEvent
    data class QuestClaimDenied(val id: String) : GameEvent
    data object DynastySwitchDenied : GameEvent
    data object CorruptionPeaked : GameEvent
    data class WonderBuilt(val id: String) : GameEvent
    data class WonderBuildDenied(val id: String) : GameEvent
    data object OmenBuffActivated : GameEvent
    data object OmenMalusActivated : GameEvent
}

/** Ephemeral (not persisted) summary of gold earned while the app was closed, shown once on return. */
data class OfflineReport(val elapsedSeconds: Long, val goldGained: Double)

class GameEngine(
    private val scope: CoroutineScope,
    private val repository: SaveRepository,
) {
    private val _state = MutableStateFlow(repository.load())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _offlineReport = MutableStateFlow<OfflineReport?>(null)
    val offlineReport: StateFlow<OfflineReport?> = _offlineReport.asStateFlow()

    init {
        applyOfflineProgress()
        scope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                tick(TICK_INTERVAL_MS / 1000.0)
                // Only the live loop rolls/counts down dragon sightings — never the offline
                // catch-up tick() call above, so a player who was away for hours doesn't get
                // hours of missed spawn rolls dumped on them at once.
                dragonTick()
                omenTick()
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
        val goldGained = totalProduction(s) * deltaSeconds
        val influenceGained = influenceProduction(s) * deltaSeconds
        val corruptionGained = corruptionGrowthPerSecond(s) * deltaSeconds
        s.copy(
            gold = s.gold + goldGained,
            lifetimeGold = s.lifetimeGold + goldGained,
            einfluss = s.einfluss + influenceGained,
            corruption = (s.corruption + corruptionGained).coerceIn(0.0, 1.0),
        )
    }

    private fun dragonTick() = mutate { s ->
        when {
            s.dragonBuffTicksRemaining > 0 -> s.copy(dragonBuffTicksRemaining = s.dragonBuffTicksRemaining - 1)
            s.dragonAvailableTicksRemaining > 0 -> s.copy(dragonAvailableTicksRemaining = s.dragonAvailableTicksRemaining - 1)
            Random.nextDouble() < DRAGON_SPAWN_CHANCE_PER_TICK -> s.copy(dragonAvailableTicksRemaining = DRAGON_AVAILABLE_TICKS)
            else -> s
        }
    }

    /** Taps a currently-visible dragon sighting, converting it into a timed production buff. */
    fun tapDragonEvent() {
        var succeeded = false
        mutate { s ->
            if (s.dragonAvailableTicksRemaining <= 0) return@mutate s
            succeeded = true
            s.copy(dragonAvailableTicksRemaining = 0, dragonBuffTicksRemaining = DRAGON_BUFF_TICKS)
        }
        if (succeeded) _events.tryEmit(GameEvent.DragonBuffActivated)
    }

    /** Only rolls while Verfall corruption is building — spawn odds rise with it. */
    private fun omenTick() = mutate { s ->
        when {
            s.omenMalusTicksRemaining > 0 -> s.copy(omenMalusTicksRemaining = s.omenMalusTicksRemaining - 1)
            s.omenBuffTicksRemaining > 0 -> s.copy(omenBuffTicksRemaining = s.omenBuffTicksRemaining - 1)
            s.omenAvailableTicksRemaining > 0 -> s.copy(omenAvailableTicksRemaining = s.omenAvailableTicksRemaining - 1)
            s.corruption > 0.0 && Random.nextDouble() < OMEN_SPAWN_CHANCE_PER_TICK * (0.5 + s.corruption) ->
                s.copy(omenAvailableTicksRemaining = OMEN_AVAILABLE_TICKS)
            else -> s
        }
    }

    /** Taps a currently-visible Verfall Omen — a coin flip between a short buff and a short malus. */
    fun tapOmenEvent() {
        var result: Boolean? = null
        mutate { s ->
            if (s.omenAvailableTicksRemaining <= 0) return@mutate s
            val isBuff = Random.nextBoolean()
            result = isBuff
            if (isBuff) {
                s.copy(omenAvailableTicksRemaining = 0, omenBuffTicksRemaining = OMEN_BUFF_TICKS)
            } else {
                s.copy(omenAvailableTicksRemaining = 0, omenMalusTicksRemaining = OMEN_MALUS_TICKS)
            }
        }
        when (result) {
            true -> _events.tryEmit(GameEvent.OmenBuffActivated)
            false -> _events.tryEmit(GameEvent.OmenMalusActivated)
            null -> {}
        }
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
        var crossedMilestones: List<Int> = emptyList()
        mutate { s ->
            val definition = BuildingDefinition.byId(buildingId)
            if (!isBuildingUnlocked(definition, s)) return@mutate s
            val level = s.buildingLevel(buildingId)
            val qty = quantity ?: maxAffordableQuantity(definition, level, s.gold, s)
            if (qty <= 0) return@mutate s
            val cost = bulkBuildingCost(definition, level, qty, s)
            if (s.gold < cost) return@mutate s
            succeeded = true
            val newLevel = level + qty
            crossedMilestones = MILESTONE_LEVELS.filter { it in (level + 1)..newLevel }
            s.copy(
                gold = s.gold - cost,
                buildings = s.buildings.map {
                    if (it.id == buildingId) it.copy(level = newLevel) else it
                },
            )
        }
        _events.tryEmit(if (succeeded) GameEvent.PurchaseSucceeded(buildingId) else GameEvent.PurchaseDenied(buildingId))
        crossedMilestones.forEach { _events.tryEmit(GameEvent.BuildingMilestoneReached(buildingId, it)) }
    }

    fun canTriggerVerfall(): Boolean = _state.value.lifetimeGold >= MIN_LIFETIME_GOLD_FOR_VERFALL

    /** Resets the current life (gold, lifetime gold, buildings) in exchange for permanent Chronicle Points. */
    fun triggerVerfall() {
        mutate { s ->
            if (s.lifetimeGold < MIN_LIFETIME_GOLD_FOR_VERFALL) return@mutate s
            val gained = chroniclePointsForVerfall(s)
            val newVerfallCount = s.verfallCount + 1
            GameState(
                chroniclePoints = s.chroniclePoints + gained,
                lifetimeChroniclePoints = s.lifetimeChroniclePoints + gained,
                academyLevel = s.academyLevel,
                verfallCount = newVerfallCount,
                sagen = s.sagen,
                wiedergeburtCount = s.wiedergeburtCount,
                activeDynastyPath = s.activeDynastyPath,
                legacyLevels = s.legacyLevels,
                unlockedAchievements = s.unlockedAchievements,
                offlineProgressEnabled = s.offlineProgressEnabled,
                lastSeenEpochSeconds = nowEpochSeconds(),
                einfluss = s.einfluss,
                ratssaalUpgrades = s.ratssaalUpgrades,
                claimedQuestIds = s.claimedQuestIds,
                lastDynastySwitchEpochSeconds = s.lastDynastySwitchEpochSeconds,
                ownedWonders = s.ownedWonders,
                currentHeirId = s.currentHeirId,
                currentHeirName = s.currentHeirName,
                chronicle = appendChronicle(s.chronicle, "blight_embraced", newVerfallCount.toString(), gained.toInt().toString()),
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
            val newWiedergeburtCount = s.wiedergeburtCount + 1
            val heir = HeirTraitDefinition.all.random()
            GameState(
                sagen = s.sagen + gained,
                wiedergeburtCount = newWiedergeburtCount,
                activeDynastyPath = s.activeDynastyPath,
                legacyLevels = s.legacyLevels,
                unlockedAchievements = s.unlockedAchievements,
                offlineProgressEnabled = s.offlineProgressEnabled,
                lastSeenEpochSeconds = nowEpochSeconds(),
                claimedQuestIds = s.claimedQuestIds,
                lastDynastySwitchEpochSeconds = s.lastDynastySwitchEpochSeconds,
                currentHeirId = heir.id,
                currentHeirName = HeirNames.random(),
                chronicle = appendChronicle(s.chronicle, "rebirth", newWiedergeburtCount.toString(), gained.toInt().toString()),
            )
        }
        persistNow()
    }

    /** Buys a one-time Ratssaal upgrade with Influence — no levels, just owned or not. */
    fun buyRatssaalUpgrade(id: String) {
        var succeeded = false
        mutate { s ->
            val definition = RatssaalUpgradeDefinition.byId(id)
            if (id in s.ratssaalUpgrades || s.einfluss < definition.cost) return@mutate s
            succeeded = true
            s.copy(einfluss = s.einfluss - definition.cost, ratssaalUpgrades = s.ratssaalUpgrades + id)
        }
        _events.tryEmit(if (succeeded) GameEvent.RatssaalUpgradePurchased(id) else GameEvent.RatssaalUpgradeDenied(id))
    }

    /** Builds a one-time Wonder with gold — no levels, gated by the Age it unlocks at. */
    fun buyWonder(id: String) {
        var succeeded = false
        mutate { s ->
            val definition = WonderDefinition.byId(id)
            if (id in s.ownedWonders || s.currentAge.index < definition.unlockAgeIndex || s.gold < definition.cost) return@mutate s
            succeeded = true
            s.copy(gold = s.gold - definition.cost, ownedWonders = s.ownedWonders + id)
        }
        _events.tryEmit(if (succeeded) GameEvent.WonderBuilt(id) else GameEvent.WonderBuildDenied(id))
    }

    /** Claims a completed quest's one-time gold reward. */
    fun claimQuest(id: String) {
        var succeeded = false
        mutate { s ->
            val definition = QuestDefinition.byId(id)
            if (id in s.claimedQuestIds || !definition.condition(s)) return@mutate s
            succeeded = true
            s.copy(gold = s.gold + definition.rewardGold, claimedQuestIds = s.claimedQuestIds + id)
        }
        _events.tryEmit(if (succeeded) GameEvent.QuestClaimed(id) else GameEvent.QuestClaimDenied(id))
    }

    fun buyLegacyUpgrade(path: DynastyPath) = mutate { s ->
        val level = s.legacyLevels[path] ?: 0
        val cost = legacyUpgradeCost(level)
        if (s.sagen < cost) return@mutate s
        s.copy(sagen = s.sagen - cost, legacyLevels = s.legacyLevels + (path to level + 1))
    }

    /** Switches the active Dynasty Path, gated by a cooldown so it stays a real commitment. */
    fun setActiveDynastyPath(path: DynastyPath) {
        var succeeded = false
        mutate { s ->
            if (s.activeDynastyPath == path) return@mutate s
            val now = nowEpochSeconds()
            if (now - s.lastDynastySwitchEpochSeconds < DYNASTY_SWITCH_COOLDOWN_SECONDS) return@mutate s
            succeeded = true
            s.copy(activeDynastyPath = path, lastDynastySwitchEpochSeconds = now)
        }
        if (!succeeded) _events.tryEmit(GameEvent.DynastySwitchDenied)
    }

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
        val newAchievements = next.unlockedAchievements - prev.unlockedAchievements
        newAchievements.forEach {
            _events.tryEmit(GameEvent.AchievementUnlocked(it))
        }
        // `>` not `!=`: triggerVerfall/triggerWiedergeburt rebuild a fresh GameState that resets
        // lifetimeGold (and therefore currentAge back down) — that must not fire a fanfare.
        val ageAdvanced = next.currentAge.index > prev.currentAge.index
        if (ageAdvanced) {
            _events.tryEmit(GameEvent.AgeAdvanced(next.currentAge.index))
        }
        val corruptionPeaked = prev.corruption < 1.0 && next.corruption >= 1.0
        if (corruptionPeaked) {
            _events.tryEmit(GameEvent.CorruptionPeaked)
        }
        // A second, separate _state.update: chronicle entries need `next`'s own values (the age
        // just reached, achievements just unlocked), which only exist once the block above ran.
        if (newAchievements.isNotEmpty() || ageAdvanced || corruptionPeaked) {
            _state.update { s ->
                var chronicle = s.chronicle
                newAchievements.forEach { chronicle = appendChronicle(chronicle, "achievement_unlocked", it) }
                if (ageAdvanced) chronicle = appendChronicle(chronicle, "age_advanced", next.currentAge.index.toString())
                if (corruptionPeaked) chronicle = appendChronicle(chronicle, "corruption_peaked")
                s.copy(chronicle = chronicle)
            }
        }
    }

    /** Appends one entry to a Chronicle list, capped at [CHRONICLE_MAX_ENTRIES]. */
    private fun appendChronicle(chronicle: List<ChronicleEntry>, kind: String, vararg args: String): List<ChronicleEntry> =
        (chronicle + ChronicleEntry(kind, nowEpochSeconds(), args.toList())).takeLast(CHRONICLE_MAX_ENTRIES)

    private fun refreshAchievements(s: GameState): GameState {
        val newlyUnlocked = AchievementDefinition.all
            .asSequence()
            .filter { it.id !in s.unlockedAchievements && it.condition(s) }
            .map { it.id }
            .toList()
        return if (newlyUnlocked.isEmpty()) s else s.copy(unlockedAchievements = s.unlockedAchievements + newlyUnlocked)
    }

    /** Dismisses the currently shown Willkommen-zurück report, if any. */
    fun acknowledgeOfflineReport() {
        _offlineReport.value = null
    }

    private fun applyOfflineProgress() {
        val now = nowEpochSeconds()
        val last = _state.value.lastSeenEpochSeconds
        if (last > 0L && _state.value.offlineProgressEnabled) {
            val elapsedSeconds = (now - last).coerceIn(0L, MAX_OFFLINE_SECONDS)
            if (elapsedSeconds > 0L) {
                val goldBefore = _state.value.gold
                tick(elapsedSeconds.toDouble())
                if (elapsedSeconds >= OFFLINE_REPORT_MIN_SECONDS) {
                    _offlineReport.value = OfflineReport(elapsedSeconds, _state.value.gold - goldBefore)
                }
            }
        }
        _state.update { it.copy(lastSeenEpochSeconds = now) }
    }

    companion object {
        private const val TICK_INTERVAL_MS = 1000L
        private const val AUTOSAVE_INTERVAL_MS = 10_000L
        private const val MAX_OFFLINE_SECONDS = 8L * 3600L
        private const val OFFLINE_REPORT_MIN_SECONDS = 60L
        // ~1/150 average odds per 1s live tick => a sighting roughly every 2.5 minutes of active play.
        private const val DRAGON_SPAWN_CHANCE_PER_TICK = 1.0 / 150.0
        private const val DRAGON_AVAILABLE_TICKS = 20
        private const val DRAGON_BUFF_TICKS = 60
        private const val CHRONICLE_MAX_ENTRIES = 200
        // ~1/300 average odds per 1s live tick at corruption 0.5 (the (0.5 + corruption) scale
        // factor) => rarer than the Dragon while corruption is low, more frequent as it climbs.
        private const val OMEN_SPAWN_CHANCE_PER_TICK = 1.0 / 300.0
        private const val OMEN_AVAILABLE_TICKS = 15
        private const val OMEN_BUFF_TICKS = 30
        private const val OMEN_MALUS_TICKS = 30
    }
}
