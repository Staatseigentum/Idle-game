package com.embercrown.game.reboot

/** Omens are invitations, not random rerolls: the next one follows the saved sequence. */
data class AshOmen(val id: String, val effect: String, val effectSeconds: Int)

object AshOmens {
    val all = listOf(
        AshOmen("caravan", "ward", 240),
        AshOmen("bell", "frenzy", 180),
        AshOmen("pyre", "pyre", 150),
    )

    fun next(sequence: Int): AshOmen = all[sequence.mod(all.size)]
    fun byId(id: String?): AshOmen? = all.firstOrNull { it.id == id }
}

fun omenWindfall(state: RebootState): Double = maxOf(250.0, production(state) * 135.0)

fun resolveOmen(state: RebootState, option: Int): RebootState {
    val omen = AshOmens.byId(state.pendingOmenId) ?: return state
    if (option !in 0..1) return state
    val sequence = state.omenSequence + 1
    val base = state.copy(
        pendingOmenId = null,
        omenSequence = sequence,
        omensResolved = state.omensResolved + 1,
        omenCountdownSeconds = 240 + (sequence * 47) % 150,
    )
    return withChronicle(if (option == 0) {
        val gained = omenWindfall(state)
        base.copy(embers = base.embers + gained, lifetimeEmbers = base.lifetimeEmbers + gained)
    } else {
        base.copy(
            omenEffectId = omen.effect,
            omenEffectSeconds = omen.effectSeconds,
            gloom = when (omen.id) {
                "caravan" -> (base.gloom - 18.0).coerceAtLeast(0.0)
                "pyre" -> (base.gloom + 15.0).coerceAtMost(100.0)
                else -> base.gloom
            },
        )
    })
}

fun enactEdict(state: RebootState, id: String): RebootState =
    if (id !in setOf("balance", "harvest", "ward", "rally") ||
        state.edictCooldownSeconds > 0 || state.edictId == id) state
    else state.copy(edictId = id, edictCooldownSeconds = 45)

/** Chronicle discoveries unlock only visual keepsakes and story, never gameplay bonuses. */
data class ChronicleEntry(val id: String, val earned: (RebootState) -> Boolean)

object CrownChronicle {
    val all = listOf(
        ChronicleEntry("kindled") { it.level("coalpit") >= 1 },
        ChronicleEntry("hundred_taps") { it.totalTaps >= 100 },
        ChronicleEntry("first_mastery") { it.buildingUpgrades.values.sum() >= 1 },
        ChronicleEntry("bells") { it.level("belltower") >= 1 },
        ChronicleEntry("beacon") { it.beaconsLit >= 5 },
        ChronicleEntry("citadel") { it.level("citadel") >= 1 },
        ChronicleEntry("three_seals") { it.claimedMilestones.size >= 3 },
        ChronicleEntry("omen_one") { it.omensResolved >= 1 },
        ChronicleEntry("omen_five") { it.omensResolved >= 5 },
        ChronicleEntry("well") { it.level("emberwell") >= 1 },
        ChronicleEntry("storm") { it.level("stormspire") >= 1 },
        ChronicleEntry("wyrm") { it.level("wyrmroost") >= 1 },
        ChronicleEntry("crown") { "crown" in it.claimedMilestones },
        ChronicleEntry("ritual") { it.reign >= 2 },
        ChronicleEntry("march_scout") { it.expeditionsCompleted >= 1 },
        ChronicleEntry("march_conquer") { it.conqueredRegions.isNotEmpty() },
        ChronicleEntry("march_specialist") { it.specializations.isNotEmpty() },
        ChronicleEntry("march_all") { it.conqueredRegions.size == LostMarches.all.size },
        ChronicleEntry("orders_three") { it.claimedOrders.size >= 3 },
        ChronicleEntry("district_one") { it.districtLevels.values.sum() >= 1 },
        ChronicleEntry("district_all") { CrownDistricts.all.all { district ->
            (it.districtLevels[district.id] ?: 0) >= CrownDistricts.MAX_LEVEL } },
        ChronicleEntry("outpost_one") { it.outpostLevels.values.sum() >= 1 },
        ChronicleEntry("artifact_one") { it.craftedArtifacts.isNotEmpty() },
        ChronicleEntry("artifacts_all") { it.craftedArtifacts.size == CrownArtifacts.all.size },
        ChronicleEntry("trial_one") { it.completedTrials.isNotEmpty() },
        ChronicleEntry("trials_all") { it.completedTrials.size == CrownTrials.all.size },
        ChronicleEntry("eclipse_siege") { it.eclipseSiegeStage >= 3 },
    )
}

fun withChronicle(state: RebootState): RebootState {
    val earned = CrownChronicle.all.filter { it.earned(state) }.map { it.id }.toSet()
    return if (earned.all { it in state.chronicleEntries }) state
    else state.copy(chronicleEntries = state.chronicleEntries + earned)
}
