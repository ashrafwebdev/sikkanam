package com.ashrafwebdev.sikkanam.inheritance

import org.junit.Assert.assertEquals
import org.junit.Test

class FaraidTest {

    private fun share(list: List<HeirShare>, key: String) = list.first { it.key == key }.total

    @Test
    fun wifeWithTwoSonsAndDaughter() {
        val s = calculateFaraid(Heirs(wives = 1, sons = 2, daughters = 1))
        assertEquals(Fraction(1, 8), share(s, "WIFE"))
        // The two sons together take 7/10, so each son takes 7/20.
        assertEquals(Fraction(7, 10), share(s, "SON"))
        assertEquals(Fraction(7, 20), s.first { it.key == "SON" }.each)
        assertEquals(Fraction(7, 40), share(s, "DAUGHTER"))
    }

    @Test
    fun husbandMotherAndFatherWithoutChildren() {
        // Umariyyatan: husband 1/2, mother 1/6 (a third of the rest), father the remainder.
        val s = calculateFaraid(Heirs(husband = true, mother = true, father = true))
        assertEquals(Fraction(1, 2), share(s, "HUSBAND"))
        assertEquals(Fraction(1, 6), share(s, "MOTHER"))
        assertEquals(Fraction(1, 3), share(s, "FATHER"))
    }

    @Test
    fun motherWithOneDaughterIsRadded() {
        // Daughter 1/2 and mother 1/6 leave 1/3, which goes back to them in proportion: 3/4 and 1/4.
        val s = calculateFaraid(Heirs(daughters = 1, mother = true))
        assertEquals(Fraction(3, 4), share(s, "DAUGHTERS"))
        assertEquals(Fraction(1, 4), share(s, "MOTHER"))
    }

    @Test
    fun husbandMotherAndTwoDaughtersAreReducedByAwl() {
        // Husband 1/4, mother 1/6 and daughters 2/3 add up to 13/12, so each is divided by 13/12.
        val s = calculateFaraid(Heirs(husband = true, mother = true, daughters = 2))
        assertEquals(Fraction(3, 13), share(s, "HUSBAND"))
        assertEquals(Fraction(2, 13), share(s, "MOTHER"))
        assertEquals(Fraction(8, 13), share(s, "DAUGHTERS"))
    }

    @Test
    fun sharesAddUpToOne() {
        val cases = listOf(
            Heirs(wives = 1, sons = 2, daughters = 1),
            Heirs(husband = true, mother = true, father = true),
            Heirs(daughters = 1, mother = true),
            Heirs(husband = true, daughters = 2),
            Heirs(father = true, mother = true, daughters = 2),
        )
        cases.forEach { heirs ->
            val total = calculateFaraid(heirs).fold(Fraction.ZERO) { acc, s -> acc + s.total }
            assertEquals(1.0, total.toDouble(), 1e-9)
        }
    }
}
