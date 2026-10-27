package com.ashrafwebdev.sikkanam.payments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentParserTest {

    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val phonepe = "com.phonepe.app"

    @Test
    fun googlePayPaidNotificationGivesAmountAndPayeeFromTitle() {
        val p = parsePayment(gpay, "Ravi", "Paid ₹500", "k1", 1L)
        assertEquals(50_000L, p?.amountCents)
        assertEquals("Ravi", p?.payee)
        assertEquals("Google Pay", p?.app)
    }

    @Test
    fun phonePeToPayeeIsRead() {
        val p = parsePayment(phonepe, "Payment successful", "₹1,250.50 paid to Ahmed Store", "k2", 1L)
        assertEquals(125_050L, p?.amountCents)
        assertEquals("Ahmed Store", p?.payee)
        assertEquals("PhonePe", p?.app)
    }

    @Test
    fun receivedMoneyIsIgnored() {
        assertNull(parsePayment(gpay, "Ravi", "Received ₹300", "k3", 1L))
    }

    @Test
    fun otherAppsAreIgnored() {
        assertNull(parsePayment("com.example.bank", "Paid", "Paid ₹500 to Ravi", "k4", 1L))
    }

    @Test
    fun notificationWithoutAmountIsIgnored() {
        assertNull(parsePayment(gpay, "Ravi", "Payment done", "k5", 1L))
    }
}
