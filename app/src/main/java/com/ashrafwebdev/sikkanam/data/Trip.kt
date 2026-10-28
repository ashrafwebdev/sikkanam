package com.ashrafwebdev.sikkanam.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A trip is a one-off tour. A family group is ongoing, and its settle-up is shown month by month. */
enum class GroupKind { TRIP, FAMILY }

/** A group that shares costs between its members. */
@Entity(tableName = "trip_groups")
data class TripGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "'TRIP'") val kind: GroupKind = GroupKind.TRIP,
    // When true, costs are split in proportion to each member's income instead of equally.
    @ColumnInfo(defaultValue = "0") val splitByIncome: Boolean = false,
)

@Entity(tableName = "trip_members")
data class TripMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    // Monthly income in cents, used only when the group splits by income.
    @ColumnInfo(defaultValue = "0") val incomeCents: Long = 0,
    // Registration details for a trip. All optional.
    @ColumnInfo(defaultValue = "''") val phone: String = "",
    @ColumnInfo(defaultValue = "''") val address: String = "",
    // Number of days this member is going on the trip.
    @ColumnInfo(defaultValue = "0") val days: Int = 0,
    // Advance paid when registering, in cents.
    @ColumnInfo(defaultValue = "0") val advanceCents: Long = 0,
    // Going members share costs. Members who only showed interest are listed but don't.
    @ColumnInfo(defaultValue = "1") val confirmed: Boolean = true,
    // The person in the master register this member is linked to. 0 for members added before linking.
    @ColumnInfo(defaultValue = "0") val personId: Long = 0,
)

/** A month of a family group that everyone has settled up. Balances carry forward only from after it. */
@Entity(tableName = "trip_settled_months")
data class TripSettledMonth(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    // Start of the settled month, as epoch millis in local time.
    val monthMillis: Long,
)

/** Money a member puts into the shared pool. */
@Entity(tableName = "trip_contributions")
data class TripContribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val memberId: Long,
    val amountCents: Long,
    val dateMillis: Long,
)

/**
 * A cost of the trip, split among its [participantIds].
 * [paidByMemberId] is null when the cost was paid from the shared pool.
 * An empty [participantIds] means everyone in the group shares it (used for older records).
 * [recurringMonthly] marks a template that repeats every month; its copies point back via [sourceId].
 */
@Entity(tableName = "trip_expenses")
data class TripExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val paidByMemberId: Long?,
    val description: String,
    val amountCents: Long,
    val dateMillis: Long,
    @ColumnInfo(defaultValue = "''") val participantIds: String = "",
    @ColumnInfo(defaultValue = "0") val recurringMonthly: Boolean = false,
    val sourceId: Long? = null,
) {
    /** The members who share this cost, as ids. */
    fun participants(): List<Long> =
        participantIds.split(',').mapNotNull { it.trim().toLongOrNull() }

    companion object {
        /** Stores a list of member ids in one column. */
        fun encode(ids: List<Long>): String = ids.joinToString(",")
    }
}
