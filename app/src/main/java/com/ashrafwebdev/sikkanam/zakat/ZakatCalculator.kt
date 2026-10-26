package com.ashrafwebdev.sikkanam.zakat

/**
 * Zakat on wealth, following the common method for money and trade goods.
 *
 * Amounts are in the user's currency. Gold and silver are given by weight in grams,
 * with the price per gram that the user enters. The app does not check the haul
 * (a full lunar year held); the user should confirm that before paying.
 */
data class ZakatInput(
    val cash: Double = 0.0,
    val goldGrams: Double = 0.0,
    val goldPricePerGram: Double = 0.0,
    val silverGrams: Double = 0.0,
    val silverPricePerGram: Double = 0.0,
    val tradeGoods: Double = 0.0,
    val moneyOwedToYou: Double = 0.0,
    val debtsDue: Double = 0.0,
    /** Silver's nisab (595 g) is lower, so it is the more cautious choice for money. */
    val useSilverNisab: Boolean = true,
)

data class ZakatResult(
    val wealth: Double,
    val nisab: Double,
    val isDue: Boolean,
    val zakat: Double,
)

object ZakatCalculator {
    private const val GOLD_NISAB_GRAMS = 85.0
    private const val SILVER_NISAB_GRAMS = 595.0
    private const val RATE = 0.025

    fun calculate(input: ZakatInput): ZakatResult {
        val gross = input.cash +
            input.goldGrams * input.goldPricePerGram +
            input.silverGrams * input.silverPricePerGram +
            input.tradeGoods +
            input.moneyOwedToYou
        val wealth = (gross - input.debtsDue).coerceAtLeast(0.0)

        val nisab = if (input.useSilverNisab) {
            SILVER_NISAB_GRAMS * input.silverPricePerGram
        } else {
            GOLD_NISAB_GRAMS * input.goldPricePerGram
        }
        val isDue = wealth > 0 && wealth >= nisab
        val zakat = if (isDue) wealth * RATE else 0.0
        return ZakatResult(wealth = wealth, nisab = nisab, isDue = isDue, zakat = zakat)
    }
}
