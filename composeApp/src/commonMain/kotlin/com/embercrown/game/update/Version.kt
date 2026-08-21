package com.embercrown.game.update

/**
 * A dotted numeric version such as `0.0.1`, tolerant of a leading `v` and of trailing
 * pre-release suffixes (`1.2.0-beta` compares as `1.2.0`).
 */
data class Version(val parts: List<Int>) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
        val size = maxOf(parts.size, other.parts.size)
        for (i in 0 until size) {
            val a = parts.getOrElse(i) { 0 }
            val b = other.parts.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    override fun toString(): String = parts.joinToString(".")

    companion object {
        /** Returns `null` when [raw] holds no leading numeric component at all. */
        fun parseOrNull(raw: String): Version? {
            val trimmed = raw.trim().removePrefix("v").removePrefix("V")
            val core = trimmed.takeWhile { it.isDigit() || it == '.' }.trim('.')
            if (core.isEmpty()) return null
            val parts = core.split('.').mapNotNull { it.toIntOrNull() }
            if (parts.isEmpty()) return null
            return Version(parts)
        }
    }
}
