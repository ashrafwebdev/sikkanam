package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity

/** A category in the master list. A name is unique within its type. */
@Entity(tableName = "categories", primaryKeys = ["name", "type"])
data class Category(
    val name: String,
    val type: TransactionType,
)
