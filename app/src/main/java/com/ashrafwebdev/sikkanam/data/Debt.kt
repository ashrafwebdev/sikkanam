package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** LENT: money you gave to someone, who owes it back. BORROWED: money you received, which you owe back. */
enum class DebtDirection { LENT, BORROWED }

/**
 * A loan (qarz) between you and one person. [paidCents] is how much has been repaid so far.
 * [dueMillis] is 0 when there is no due date. The amount still owed is amountCents minus paidCents.
 */
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: DebtDirection,
    val amountCents: Long,
    val paidCents: Long = 0,
    val dateMillis: Long,
    val dueMillis: Long = 0,
    val note: String = "",
) {
    val remainingCents: Long get() = (amountCents - paidCents).coerceAtLeast(0L)
}
