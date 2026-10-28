package com.ashrafwebdev.sikkanam.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Where a person is in the dawat and taskeel process. Listed in the order they usually happen. */
enum class DawatStatus {
    MET,
    INTERESTED,
    TASKEEL_MUQAMI,
    TASKEEL_KHUROOJ,
    AGREED_KHUROOJ,
    NOT_NOW,
    ;

    /** The next status when the user taps the status. */
    fun next(): DawatStatus = entries[(ordinal + 1) % entries.size]
}

/**
 * A person met in dawat. [followUpMillis] is 0 when there is no follow-up date.
 * Once the person agrees to khurooj, they can be added to a new jamaat.
 */
@Entity(tableName = "dawat_contacts")
data class DawatContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val area: String = "",
    val status: DawatStatus = DawatStatus.MET,
    val metMillis: Long,
    val followUpMillis: Long = 0,
    val notes: String = "",
    // The person in the master register this contact is linked to. 0 for records made before linking.
    @ColumnInfo(defaultValue = "0") val personId: Long = 0,
)
