package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One amount put toward a savings goal. The goal's saved total is the sum of its entries. */
@Entity(tableName = "goal_entries")
data class GoalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amountCents: Long,
    val dateMillis: Long,
    val note: String = "",
)
