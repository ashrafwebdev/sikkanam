package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A duty in the list. The list starts with [Duties.defaults] and can be edited. */
@Entity(tableName = "duty_types")
data class DutyType(
    @PrimaryKey val name: String,
    val sortOrder: Int,
)

/** The default duties (responsibilities) shared out in the daily mashwara. */
object Duties {
    val defaults = listOf(
        "Mashwara Aadhaab",
        "Khususi mulaaqat",
        "Khidmat",
        "Taleem",
        "Elaan",
        "Taruf baat",
        "Aadhaabe Gasht",
        "Darmiyani baat",
        "Deen ki baat",
        "Usool Mudhaakara",
        "Kaane ke Aadhaab",
        "Sone ke Aadhaab",
        "Tahajjud",
        "Fajri ki baat",
        "Che number Mudhaakara",
    )

    /** Adds the default duties to the duty list, in order. */
    fun insertDefaults(execSql: (String, Array<Any>) -> Unit) {
        defaults.forEachIndexed { index, name ->
            execSql("INSERT OR IGNORE INTO duty_types (name, sortOrder) VALUES (?, ?)", arrayOf(name, index))
        }
    }
}

/**
 * One duty given to one person on one day. The mashwara decides each day who takes each duty.
 * [done] is ticked when the duty is carried out.
 */
@Entity(tableName = "duty_assignments")
data class DutyAssignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Start of the day this duty belongs to, as epoch millis in local time.
    val dayMillis: Long,
    val duty: String,
    val personName: String,
    val done: Boolean = false,
)
