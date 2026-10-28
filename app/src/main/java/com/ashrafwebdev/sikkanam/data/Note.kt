package com.ashrafwebdev.sikkanam.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A personal note: a title and a text. [updatedMillis] is when it was last saved.
 * A [pinned] note is listed above the others.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val updatedMillis: Long,
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
)
