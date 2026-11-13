package com.ashrafwebdev.sikkanam.payments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedPaymentTextTest {

    @Test
    fun paidTextGivesExpenseAmountAndPayee() {
        val p = parseSharedPaymentText("I paid ₹1,250.50 to Ahmed Store using Google Pay")
        assertEquals(125_050L, p.amountCents)
        assertEquals("Ahmed Store", p.party)
        assertEquals(false, p.isIncome)
    }

    @Test
    fun receivedTextGivesIncomeAndSender() {
        val p = parseSharedPaymentText("Received Rs. 300 from Ravi on 12 Nov")
        assertEquals(30_000L, p.amountCents)
        assertEquals("Ravi", p.party)
        assertEquals(true, p.isIncome)
    }

    @Test
    fun unknownTextLeavesEverythingEmpty() {
        val p = parseSharedPaymentText("Check out this app")
        assertNull(p.amountCents)
        assertEquals("", p.party)
        assertNull(p.isIncome)
    }
}
