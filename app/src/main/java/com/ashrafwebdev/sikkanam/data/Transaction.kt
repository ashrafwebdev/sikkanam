package com.ashrafwebdev.sikkanam.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE }

/** Islamic status of an income entry. Only meaningful for income. */
enum class HalalStatus { NONE, HALAL, DOUBTFUL, HARAM }

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    // Stored in minor units (cents) so money never suffers floating-point drift.
    val amountCents: Long,
    val category: String,
    val note: String,
    val dateMillis: Long,
    @ColumnInfo(defaultValue = "'NONE'") val halalStatus: HalalStatus = HalalStatus.NONE,
    // Marks an expense that is interest (riba), so the app can total it.
    @ColumnInfo(defaultValue = "0") val isRiba: Boolean = false,
    // A template that repeats every month. Its copies point back to it through sourceId.
    @ColumnInfo(defaultValue = "0") val recurringMonthly: Boolean = false,
    val sourceId: Long? = null,
    // Who the money came from (income) or went to (expense).
    @ColumnInfo(defaultValue = "''") val party: String = "",
    // How the money was paid or received, such as cash or bank transfer.
    @ColumnInfo(defaultValue = "''") val paymentMethod: String = "",
    // The account the money went into or came out of. Null means no account is linked.
    val accountId: Long? = null,
    // File name of a receipt photo kept in the app's private folder. Null when there is none.
    val receiptPath: String? = null,
)
