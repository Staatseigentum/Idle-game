package com.embercrown.game.reboot

/** Purely visual rewards. None of these IDs is consulted by economy or progression code. */
data class ChronicleLook(val category: String, val id: String, val achievement: String? = null)

object ChronicleCosmetics {
    val all = listOf(
        ChronicleLook("flame", "ember"),
        ChronicleLook("flame", "moonfire", "beacon"),
        ChronicleLook("flame", "witchfire", "omen_five"),
        ChronicleLook("flame", "ghostfire", "artifacts_all"),
        ChronicleLook("banner", "ash"),
        ChronicleLook("banner", "master", "first_mastery"),
        ChronicleLook("banner", "march", "march_conquer"),
        ChronicleLook("banner", "royal", "three_seals"),
        ChronicleLook("banner", "eclipse", "eclipse_siege"),
        ChronicleLook("sky", "blood"),
        ChronicleLook("sky", "storm", "storm"),
        ChronicleLook("sky", "veil", "trial_one"),
        ChronicleLook("sky", "eclipse", "eclipse_siege"),
        ChronicleLook("map", "iron"),
        ChronicleLook("map", "warden", "march_conquer"),
        ChronicleLook("map", "gilded", "march_all"),
    )
    val categories = listOf("flame", "banner", "sky", "map")

    fun options(category: String): List<ChronicleLook> = all.filter { it.category == category }
    fun unlocked(state: RebootState, look: ChronicleLook): Boolean =
        look.achievement == null || look.achievement in state.chronicleEntries
}

fun cosmeticStyle(state: RebootState, category: String): String {
    val selected = state.cosmeticStyles[category]
    val look = ChronicleCosmetics.options(category).firstOrNull { it.id == selected }
    return if (look != null && ChronicleCosmetics.unlocked(state, look)) look.id
        else ChronicleCosmetics.options(category).firstOrNull()?.id.orEmpty()
}

fun selectCosmetic(state: RebootState, category: String, id: String): RebootState {
    val look = ChronicleCosmetics.options(category).firstOrNull { it.id == id } ?: return state
    if (!ChronicleCosmetics.unlocked(state, look)) return state
    if (cosmeticStyle(state, category) == id) return state
    return state.copy(cosmeticStyles = state.cosmeticStyles + (category to id))
}

/** The first four unlocked sigils appear by default; players can curate a shelf of four. */
fun featuredTrophies(state: RebootState): List<String> =
    if (state.featuredTrophies != null) state.featuredTrophies.filter { it in state.chronicleEntries }.take(4)
    else CrownChronicle.all.map { it.id }.filter { it in state.chronicleEntries }.take(4)

fun toggleFeaturedTrophy(state: RebootState, id: String): RebootState {
    if (id !in state.chronicleEntries || CrownChronicle.all.none { it.id == id }) return state
    val current = featuredTrophies(state)
    val next = if (id in current) current - id else (current + id).takeLast(4)
    return state.copy(featuredTrophies = next)
}
