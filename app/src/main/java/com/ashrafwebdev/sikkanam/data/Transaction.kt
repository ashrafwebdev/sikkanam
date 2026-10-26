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
)
