package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A daily mashwara (consultation meeting): the date, topic, who attended, and the notes. */
@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val topic: String,
    // Names of the people who attended, separated by commas.
    val attendees: String,
    val notes: String,
    // The jamaat this meeting belongs to. 0 means not linked to a jamaat (older records).
    val groupId: Long = 0,
)
