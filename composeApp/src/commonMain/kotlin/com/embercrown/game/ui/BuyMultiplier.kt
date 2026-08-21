package com.embercrown.game.ui

enum class BuyMultiplier(val quantity: Int?, val label: String) {
    X1(1, "×1"),
    X10(10, "×10"),
    X100(100, "×100"),
    MAX(null, "MAX"),
}
