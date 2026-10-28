package com.ashrafwebdev.sikkanam.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A person living in the masjid's locality. [wentFortyDays] and [wentFourMonths] record whether they have
 * already gone on that khurooj. [active] is whether they take part now, so the locality can show how many
 * are active and how many are inactive.
 */
@Entity(tableName = "locality_people")
data class LocalityPerson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val locality: String = "",
    val wentFortyDays: Boolean = false,
    val wentFourMonths: Boolean = false,
    val active: Boolean = false,
    val notes: String = "",
    // The khurooj types this person is available for, as comma-separated KhuroojType names.
    val availableTypes: String = "",
    // The person in the master register this entry is linked to. 0 for records made before linking.
    @ColumnInfo(defaultValue = "0") val personId: Long = 0,
)

/**
 * One time you met a person from the register. [dayMillis] is the start of that day. [taskeel] is the
 * khurooj type given as taskeel at that meeting, as a KhuroojType name, or empty for none.
 */
@Entity(tableName = "meeting_visits")
data class MeetingVisit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val dayMillis: Long,
    val note: String = "",
    val taskeel: String = "",
)
