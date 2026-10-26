package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Money being put aside toward a target, such as a wedding or a house deposit. */
@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetCents: Long,
    val savedCents: Long,
)
