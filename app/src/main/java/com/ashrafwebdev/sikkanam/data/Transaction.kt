package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE }

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    // Stored in minor units (cents) so money never suffers floating-point drift.
    val amountCents: Long,
    val category: String,
    val note: String,
    val dateMillis: Long,
)
