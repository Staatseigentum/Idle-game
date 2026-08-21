package com.embercrown.game.ui

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

private val SUFFIXES = listOf("K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc")

fun formatAmount(value: Double): String {
    if (value < 1000.0) return formatPlain(value)

    var scaled = value
    var suffixIndex = -1
    while (scaled >= 1000.0 && suffixIndex < SUFFIXES.lastIndex) {
        scaled /= 1000.0
        suffixIndex++
    }
    val rounded = roundTo(scaled, 2)
    return "$rounded${SUFFIXES[suffixIndex]}"
}

private fun formatPlain(value: Double): String {
    val floored = floor(value)
    return if (value == floored) floored.toLong().toString() else roundTo(value, 1).toString()
}

private fun roundTo(value: Double, decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return round(value * factor) / factor
}
