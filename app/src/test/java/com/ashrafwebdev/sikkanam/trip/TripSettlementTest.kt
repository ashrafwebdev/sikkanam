package com.ashrafwebdev.sikkanam.trip

import com.ashrafwebdev.sikkanam.data.TripContribution
import com.ashrafwebdev.sikkanam.data.TripExpense
import com.ashrafwebdev.sikkanam.data.TripMember
import org.junit.Assert.assertEquals
import org.junit.Test

class TripSettlementTest {

    private val alice = TripMember(id = 1, groupId = 1, name = "Alice")
    private val bob = TripMember(id = 2, groupId = 1, name = "Bob")
    private val carol = TripMember(id = 3, groupId = 1, name = "Carol")
    private val members = listOf(alice, bob, carol)

    @Test
    fun pooledMoneyCoversExpensesAndLeftoverIsRefunded() {
        // Each contributes 300.00, and 450.00 is spent from the pool. Each share is 150.00.
        val contributions = members.map { TripContribution(groupId = 1, memberId = it.id, amountCents = 30_000, dateMillis = 0) }
        val expenses = listOf(TripExpense(groupId = 1, paidByMemberId = null, description = "Hotel", amountCents = 45_000, dateMillis = 0))

        val balances = TripSettlement.balances(members, contributions, expenses)
        val payments = TripSettlement.settlements(balances)

        // Pool held 900.00 and spent 450.00, so 450.00 goes back, 150.00 to each member.
        assertEquals(listOf(15_000L, 15_000L, 15_000L), balances.map { it.net })
        assertEquals(3, payments.size)
        assertEquals(setOf("Alice", "Bob", "Carol"), payments.map { it.to }.toSet())
        assertEquals(true, payments.all { it.from == "Pool" && it.amountCents == 15_000L })
    }

    @Test
    fun someoneWhoPaidForEveryoneIsRepaidByTheRest() {
        // Alice pays 300.00 of expenses herself. Each share is 100.00, so Bob and Carol each owe her 100.00.
        val expenses = listOf(TripExpense(groupId = 1, paidByMemberId = alice.id, description = "Tickets", amountCents = 30_000, dateMillis = 0))

        val balances = TripSettlement.balances(members, emptyList(), expenses)
        val payments = TripSettlement.settlements(balances)

        assertEquals(20_000L, balances.first { it.name == "Alice" }.net)
        assertEquals(2, payments.size)
        assertEquals(true, payments.all { it.to == "Alice" && it.amountCents == 10_000L })
        assertEquals(setOf("Bob", "Carol"), payments.map { it.from }.toSet())
    }

    @Test
    fun expenseSharedBySomeMembersIsSplitAmongThem() {
        // 300.00 for Alice and Bob only, so each owes 150.00. Carol owes nothing for it.
        val expenses = listOf(
            TripExpense(
                groupId = 1, paidByMemberId = alice.id, description = "Dinner", amountCents = 30_000,
                dateMillis = 0, participantIds = "1,2",
            )
        )

        val balances = TripSettlement.balances(members, emptyList(), expenses)

        assertEquals(15_000L, balances.first { it.name == "Bob" }.share)
        assertEquals(0L, balances.first { it.name == "Carol" }.share)
        assertEquals(15_000L, balances.first { it.name == "Alice" }.share)
        assertEquals(30_000L, balances.sumOf { it.share })
    }

    @Test
    fun costsSplitByIncomeInProportion() {
        // Alice earns 3000.00 and Bob 1000.00, so a 4000.00 cost splits 3000.00 and 1000.00.
        val incomeMembers = listOf(
            alice.copy(incomeCents = 300_000),
            bob.copy(incomeCents = 100_000),
        )
        val expenses = listOf(TripExpense(groupId = 1, paidByMemberId = null, description = "Rent", amountCents = 400_000, dateMillis = 0))

        val balances = TripSettlement.balances(incomeMembers, emptyList(), expenses, byIncome = true)

        assertEquals(300_000L, balances.first { it.name == "Alice" }.share)
        assertEquals(100_000L, balances.first { it.name == "Bob" }.share)
    }

    @Test
    fun incomeSplitFallsBackToEqualWhenNoIncomeIsRecorded() {
        val expenses = listOf(TripExpense(groupId = 1, paidByMemberId = null, description = "Gas", amountCents = 30_000, dateMillis = 0))
        val balances = TripSettlement.balances(members, emptyList(), expenses, byIncome = true)
        assertEquals(listOf(10_000L, 10_000L, 10_000L), balances.map { it.share })
    }

    @Test
    fun balanceBroughtForwardStillNeedsSettling() {
        // Bob owed Alice 100.00 last month and has not paid, so this month he still pays her.
        val balances = TripSettlement.balances(
            members.take(2), emptyList(), emptyList(), carriedIn = mapOf(alice.id to 10_000L, bob.id to -10_000L),
        )
        val payments = TripSettlement.settlements(balances)

        assertEquals(listOf(Payment("Bob", "Alice", 10_000L)), payments)
    }

    @Test
    fun sharesAddUpToTheTotalEvenWhenCentsDoNotDivide() {
        // 100.00 split three ways is 33.34, 33.33, 33.33.
        val expenses = listOf(TripExpense(groupId = 1, paidByMemberId = null, description = "Food", amountCents = 10_000, dateMillis = 0))
        val balances = TripSettlement.balances(members, emptyList(), expenses)
        assertEquals(10_000L, balances.sumOf { it.share })
    }
}
