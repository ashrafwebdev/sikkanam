package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Where money is kept, such as cash, a bank account, or a card. */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // Balance when the account was added. Transactions and transfers move it from there.
    val openingCents: Long,
)

/** Money moved from one account to another. It doesn't count as income or expense. */
@Entity(tableName = "transfers")
data class Transfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amountCents: Long,
    val dateMillis: Long,
    val note: String,
)
