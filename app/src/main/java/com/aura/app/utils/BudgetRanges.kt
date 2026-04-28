package com.aura.app.utils

object BudgetRanges {
    val RANGES = listOf(
        "$1000 - $5000",
        "$5000 - $10,000",
        "$10,000+"
    )

    fun toMinMaxCents(range: String): Pair<Long, Long> = when (range) {
        "$1000 - $5000" -> 100_000L to 500_000L
        "$5000 - $10,000" -> 500_000L to 1_000_000L
        "$10,000+" -> 1_000_000L to 0L
        else -> 0L to 0L
    }
}
