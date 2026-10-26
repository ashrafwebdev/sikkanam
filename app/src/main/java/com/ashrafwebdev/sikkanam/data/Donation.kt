package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DonationKind { SADAQAH, PURIFICATION }

/** A charity payment: voluntary sadaqah, or purification of haram income. */
@Entity(tableName = "donations")
data class Donation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: DonationKind,
    val amountCents: Long,
    val note: String,
    val dateMillis: Long,
)
