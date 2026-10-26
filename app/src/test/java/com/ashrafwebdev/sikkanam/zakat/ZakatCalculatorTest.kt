package com.ashrafwebdev.sikkanam.zakat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZakatCalculatorTest {

    @Test
    fun wealthAboveSilverNisabPaysTwoPointFivePercent() {
        // Silver nisab = 595 g * 1.0 = 595. Wealth 10,000 is above it.
        val result = ZakatCalculator.calculate(
            ZakatInput(cash = 10_000.0, silverPricePerGram = 1.0)
        )
        assertTrue(result.isDue)
        assertEquals(250.0, result.zakat, 0.001)
    }

    @Test
    fun wealthBelowNisabPaysNothing() {
        val result = ZakatCalculator.calculate(
            ZakatInput(cash = 100.0, silverPricePerGram = 1.0)
        )
        assertFalse(result.isDue)
        assertEquals(0.0, result.zakat, 0.001)
    }

    @Test
    fun debtsAreSubtractedBeforeTheNisabCheck() {
        // 1,000 cash less 500 debts = 500, which is below the silver nisab of 595.
        val result = ZakatCalculator.calculate(
            ZakatInput(cash = 1_000.0, debtsDue = 500.0, silverPricePerGram = 1.0)
        )
        assertEquals(500.0, result.wealth, 0.001)
        assertFalse(result.isDue)
    }

    @Test
    fun goldNisabIsUsedWhenSelected() {
        // Gold nisab = 85 g * 100 = 8,500. Wealth 9,000 is above it, so zakat is due.
        val result = ZakatCalculator.calculate(
            ZakatInput(cash = 9_000.0, goldPricePerGram = 100.0, useSilverNisab = false)
        )
        assertEquals(8_500.0, result.nisab, 0.001)
        assertTrue(result.isDue)
    }

    @Test
    fun negativeWealthIsClampedToZero() {
        val result = ZakatCalculator.calculate(ZakatInput(cash = 100.0, debtsDue = 900.0))
        assertEquals(0.0, result.wealth, 0.001)
        assertFalse(result.isDue)
    }
}
