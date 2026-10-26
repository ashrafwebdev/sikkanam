package com.ashrafwebdev.sikkanam.trip

import com.ashrafwebdev.sikkanam.data.TripContribution
import com.ashrafwebdev.sikkanam.data.TripExpense
import com.ashrafwebdev.sikkanam.data.TripMember
import java.math.BigInteger

/**
 * One member's position in a trip, in cents.
 * [net] is what they are owed (positive) or what they owe (negative).
 */
data class MemberBalance(
    val memberId: Long,
    val name: String,
    val contributed: Long,
    val paidDirectly: Long,
    val share: Long,
    // Unsettled balance brought forward from earlier months.
    val carried: Long = 0,
) {
    val net: Long get() = contributed + paidDirectly - share + carried
}

/** Someone pays someone. "Pool" means money held back from the shared pool. */
data class Payment(val from: String, val to: String, val amountCents: Long)

object TripSettlement {
    private const val POOL = "Pool"

    /**
     * Each expense is split equally among its participants. Cents that don't divide evenly
     * go to the first participants in the list, so the shares always add up to the cost.
     * An expense with no participants recorded is shared by everyone in the group.
     */
    fun balances(
        members: List<TripMember>,
        contributions: List<TripContribution>,
        expenses: List<TripExpense>,
        byIncome: Boolean = false,
        carriedIn: Map<Long, Long> = emptyMap(),
    ): List<MemberBalance> {
        if (members.isEmpty()) return emptyList()
        val memberIds = members.map { it.id }
        val incomeById = members.associate { it.id to it.incomeCents }
        val shares = memberIds.associateWith { 0L }.toMutableMap()

        expenses.forEach { expense ->
            val sharers = expense.participants().filter { it in memberIds }.ifEmpty { memberIds }
            // Split by income only when the sharers have income recorded; otherwise split equally.
            val useIncome = byIncome && sharers.sumOf { incomeById.getValue(it) } > 0
            val weights = sharers.map { if (useIncome) incomeById.getValue(it) else 1L }
            val parts = splitByWeights(expense.amountCents, weights)
            sharers.forEachIndexed { index, id ->
                shares[id] = shares.getValue(id) + parts[index]
            }
        }

        return members.map { member ->
            MemberBalance(
                memberId = member.id,
                name = member.name,
                contributed = contributions.filter { it.memberId == member.id }.sumOf { it.amountCents },
                paidDirectly = expenses.filter { it.paidByMemberId == member.id }.sumOf { it.amountCents },
                share = shares.getValue(member.id),
                carried = carriedIn[member.id] ?: 0L,
            )
        }
    }

    /**
     * Splits [amount] in proportion to [weights], exactly: the parts always add up to the amount.
     * Leftover cents go to the first parts. Uses BigInteger so large amounts can't overflow.
     */
    private fun splitByWeights(amount: Long, weights: List<Long>): List<Long> {
        val total = BigInteger.valueOf(weights.sum())
        val floors = weights.map { w ->
            BigInteger.valueOf(amount).multiply(BigInteger.valueOf(w)).divide(total).toLong()
        }
        val leftover = amount - floors.sum()
        return floors.mapIndexed { index, part -> if (index < leftover) part + 1 else part }
    }

    /**
     * The fewest payments that settle everyone: people who owe pay people who are owed.
     * Anything left over is a refund from the pool to someone owed, or money a debtor
     * still owes the pool.
     */
    fun settlements(balances: List<MemberBalance>): List<Payment> {
        val creditors = balances.filter { it.net > 0 }.map { it.name to it.net }.sortedByDescending { it.second }.toMutableList()
        val debtors = balances.filter { it.net < 0 }.map { it.name to -it.net }.sortedByDescending { it.second }.toMutableList()
        val payments = mutableListOf<Payment>()

        var c = 0
        var d = 0
        while (c < creditors.size && d < debtors.size) {
            val pay = minOf(creditors[c].second, debtors[d].second)
            payments.add(Payment(debtors[d].first, creditors[c].first, pay))
            creditors[c] = creditors[c].first to creditors[c].second - pay
            debtors[d] = debtors[d].first to debtors[d].second - pay
            if (creditors[c].second == 0L) c++
            if (debtors[d].second == 0L) d++
        }
        while (c < creditors.size) {
            payments.add(Payment(POOL, creditors[c].first, creditors[c].second))
            c++
        }
        while (d < debtors.size) {
            payments.add(Payment(debtors[d].first, POOL, debtors[d].second))
            d++
        }
        return payments
    }
}
