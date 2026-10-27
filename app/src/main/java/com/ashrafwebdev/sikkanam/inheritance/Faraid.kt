package com.ashrafwebdev.sikkanam.inheritance

/** Exact fractions, so that shares add up to exactly 1 with no rounding. */
data class Fraction(val num: Long, val den: Long) : Comparable<Fraction> {
    init {
        require(den != 0L)
    }

    override fun compareTo(other: Fraction): Int = (num * other.den).compareTo(other.num * den)

    operator fun plus(o: Fraction) = of(num * o.den + o.num * den, den * o.den)
    operator fun minus(o: Fraction) = of(num * o.den - o.num * den, den * o.den)
    operator fun times(o: Fraction) = of(num * o.num, den * o.den)
    operator fun div(o: Fraction) = of(num * o.den, den * o.num)
    operator fun times(n: Long) = of(num * n, den)
    fun toDouble(): Double = num.toDouble() / den

    companion object {
        val ZERO = Fraction(0, 1)
        val ONE = Fraction(1, 1)
        fun of(n: Long, d: Long): Fraction {
            val g = gcd(kotlin.math.abs(n), kotlin.math.abs(d)).coerceAtLeast(1)
            val sign = if (d < 0) -1 else 1
            return Fraction(sign * n / g, sign * d / g)
        }
        private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    }
}

/** Who is alive and inheriting. Counts are for people of that relation. */
data class Heirs(
    val husband: Boolean = false,
    val wives: Int = 0,
    val sons: Int = 0,
    val daughters: Int = 0,
    val father: Boolean = false,
    val mother: Boolean = false,
)

/** One group of heirs, with the total fraction for the group and how many people share it. */
data class HeirShare(val key: String, val count: Int, val total: Fraction) {
    val each: Fraction get() = if (count == 0) Fraction.ZERO else total / Fraction.of(count.toLong(), 1)
}

/**
 * Shares under Islamic inheritance (faraid) for the heirs listed in [Heirs]. Covers the spouse, parents,
 * sons and daughters. Awl (when the shares add up to more than 1) and radd (when they add up to less)
 * are applied. Other relatives (grandchildren, brothers, sisters, grandparents) are not included yet, so
 * complicated cases must be checked with a qualified scholar.
 */
fun calculateFaraid(h: Heirs): List<HeirShare> {
    val children = h.sons + h.daughters > 0
    val fixed = linkedMapOf<String, Fraction>()

    if (h.husband) fixed["HUSBAND"] = if (children) Fraction.of(1, 4) else Fraction.of(1, 2)
    if (h.wives > 0) fixed["WIFE"] = if (children) Fraction.of(1, 8) else Fraction.of(1, 4)

    if (h.mother) {
        fixed["MOTHER"] = when {
            children -> Fraction.of(1, 6)
            // Umariyyatan: with a spouse and father, the mother takes one third of what remains after the spouse.
            h.father && (h.husband || h.wives > 0) -> {
                val spouse = fixed["HUSBAND"] ?: fixed["WIFE"] ?: Fraction.ZERO
                (Fraction.ONE - spouse) * Fraction.of(1, 3)
            }
            else -> Fraction.of(1, 3)
        }
    }
    if (h.father && children) fixed["FATHER"] = Fraction.of(1, 6)
    if (h.sons == 0 && h.daughters > 0) {
        fixed["DAUGHTERS"] = if (h.daughters == 1) Fraction.of(1, 2) else Fraction.of(2, 3)
    }

    val sum = fixed.values.fold(Fraction.ZERO) { acc, f -> acc + f }
    val result = linkedMapOf<String, HeirShare>()

    fun put(key: String, count: Int, total: Fraction) {
        if (count > 0) result[key] = HeirShare(key, count, total)
    }

    when {
        // Awl: the fixed shares are too big, so every share is reduced in the same proportion.
        sum > Fraction.ONE -> {
            fixed.forEach { (key, f) ->
                val count = when (key) {
                    "HUSBAND", "MOTHER", "FATHER" -> 1
                    "WIFE" -> h.wives
                    "DAUGHTERS" -> h.daughters
                    else -> 1
                }
                put(key, count, f / sum)
            }
        }
        // Sons and daughters take the rest, two parts for each son and one for each daughter.
        h.sons > 0 -> {
            val rest = Fraction.ONE - sum
            val parts = (2 * h.sons + h.daughters).toLong()
            fixed.forEach { (key, f) -> put(key, countOf(key, h), f) }
            put("SON", h.sons, rest * Fraction.of(2L * h.sons, parts))
            put("DAUGHTER", h.daughters, rest * Fraction.of(h.daughters.toLong(), parts))
        }
        // No sons: the father takes the rest, as the nearest male relative.
        h.father -> {
            fixed.forEach { (key, f) -> put(key, countOf(key, h), if (key == "FATHER") f + (Fraction.ONE - sum) else f) }
            if (!fixed.containsKey("FATHER")) put("FATHER", 1, Fraction.ONE - sum)
        }
        // Nobody takes the rest, so it goes back to the other heirs in proportion (radd).
        // The spouse does not take radd when other heirs are present.
        else -> {
            val others = fixed.filterKeys { it != "HUSBAND" && it != "WIFE" }
            val rest = Fraction.ONE - sum
            if (others.isEmpty()) {
                fixed.forEach { (key, f) -> put(key, countOf(key, h), f) }
                put(if (h.husband) "HUSBAND" else "WIFE", countOf(if (h.husband) "HUSBAND" else "WIFE", h), rest)
            } else {
                val othersSum = others.values.fold(Fraction.ZERO) { acc, f -> acc + f }
                fixed.forEach { (key, f) ->
                    val extra = if (others.containsKey(key)) rest * (f / othersSum) else Fraction.ZERO
                    put(key, countOf(key, h), f + extra)
                }
            }
        }
    }
    return result.values.toList()
}

private fun countOf(key: String, h: Heirs): Int = when (key) {
    "WIFE" -> h.wives
    "DAUGHTERS" -> h.daughters
    else -> 1
}
