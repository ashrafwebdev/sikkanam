package com.ashrafwebdev.sikkanam.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** The women's efforts: how long a trip lasts. */
enum class MastooratEffort { DAYS_3, DAYS_10, DAYS_40, MONTHS_2_FOREIGN }

/** Weekly ijtima of taleem in one house, or the daily taleem in a house. */
enum class TaleemKind { WEEKLY_IJTIMA, DAILY_HOME }

/** Women who joined a tashkeel ijtima with their mahram men, by how long they went out. */
enum class TashkeelKind { THREE_TIMES_THREE_DAYS, TEN_DAYS, FORTY_DAYS, TWO_MONTHS }

/** A trip of the Mastoorat effort. Its pairs are fixed when it is made, unless the effort allows more. */
@Entity(tableName = "mastoorat_trips")
data class MastooratTrip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val effort: MastooratEffort,
    val startMillis: Long,
    val endMillis: Long,
    val place: String = "",
)

/** One pair going out: a woman and her mahram. [childAge] is the age of the female child they bring, or 0 for none. */
@Entity(tableName = "mastoorat_pairs")
data class MastooratPair(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val woman: String,
    val mahram: String,
    val childAge: Int = 0,
)

/** One taleem meeting in a house: the house, the day, and how many women came. */
@Entity(tableName = "mastoorat_taleem")
data class MastooratTaleem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val kind: TaleemKind,
    val dayMillis: Long,
    val house: String = "",
    val attended: Int = 0,
)

/** Women who went out for tashkeel on [dayMillis], counted by how long they went. */
@Entity(tableName = "mastoorat_tashkeel")
data class MastooratTashkeel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val dayMillis: Long,
    val kind: TashkeelKind,
    val joined: Int = 0,
)

/** A pair as it is entered, before it is saved. */
data class PairInput(val woman: String, val mahram: String, val childAge: Int)

/** Why a set of pairs breaks the Mastoorat limits. */
enum class PairProblem { TOO_MANY_PAIRS, TOO_MANY_CHILD_PAIRS, CHILD_TOO_YOUNG }

/** The limits set for the Mastoorat efforts. */
object MastooratRules {
    const val MAX_PAIRS = 5
    const val MAX_CHILD_PAIRS = 2
    const val MIN_CHILD_AGE = 9
    const val TALEEM_MIN_ATTENDED = 20
    const val TALEEM_MAX_ATTENDED = 25

    /** Short efforts (3 and 10 days) can take more pairs after the trip is made. Longer ones cannot. */
    fun canAddPairsLater(effort: MastooratEffort): Boolean =
        effort == MastooratEffort.DAYS_3 || effort == MastooratEffort.DAYS_10

    /** Checks the pairs against the limits. Returns null when they are allowed. */
    fun check(pairs: List<PairInput>): PairProblem? {
        if (pairs.size > MAX_PAIRS) return PairProblem.TOO_MANY_PAIRS
        val withChild = pairs.filter { it.childAge != 0 }
        if (withChild.any { it.childAge < MIN_CHILD_AGE }) return PairProblem.CHILD_TOO_YOUNG
        if (withChild.size > MAX_CHILD_PAIRS) return PairProblem.TOO_MANY_CHILD_PAIRS
        return null
    }

    /** Taleem in a house is on target when between 20 and 25 women came. */
    fun taleemOnTarget(attended: Int): Boolean = attended in TALEEM_MIN_ATTENDED..TALEEM_MAX_ATTENDED
}

@Dao
interface MastooratDao {
    @Query("SELECT * FROM mastoorat_trips ORDER BY startMillis DESC")
    fun observeTrips(): Flow<List<MastooratTrip>>

    @Query("SELECT * FROM mastoorat_pairs ORDER BY id")
    fun observePairs(): Flow<List<MastooratPair>>

    @Query("SELECT * FROM mastoorat_taleem ORDER BY dayMillis DESC")
    fun observeTaleem(): Flow<List<MastooratTaleem>>

    @Query("SELECT * FROM mastoorat_tashkeel ORDER BY dayMillis DESC")
    fun observeTashkeel(): Flow<List<MastooratTashkeel>>

    @Insert
    suspend fun insertTrip(trip: MastooratTrip): Long

    @Insert
    suspend fun insertPairs(pairs: List<MastooratPair>)

    @Query("DELETE FROM mastoorat_pairs WHERE tripId = :tripId")
    suspend fun deletePairsOfTrip(tripId: Long)

    @Query("DELETE FROM mastoorat_trips WHERE id = :id")
    suspend fun deleteTrip(id: Long)

    @Insert
    suspend fun insertTaleem(entry: MastooratTaleem)

    @Query("DELETE FROM mastoorat_taleem WHERE id = :id")
    suspend fun deleteTaleem(id: Long)

    @Insert
    suspend fun insertTashkeel(entry: MastooratTashkeel)

    @Query("DELETE FROM mastoorat_tashkeel WHERE id = :id")
    suspend fun deleteTashkeel(id: Long)

    @Query("DELETE FROM mastoorat_pairs WHERE tripId IN (SELECT id FROM mastoorat_trips WHERE groupId = :groupId)")
    suspend fun deletePairsOfGroup(groupId: Long)

    @Query("DELETE FROM mastoorat_trips WHERE groupId = :groupId")
    suspend fun deleteTripsOfGroup(groupId: Long)

    @Query("DELETE FROM mastoorat_taleem WHERE groupId = :groupId")
    suspend fun deleteTaleemOfGroup(groupId: Long)

    @Query("DELETE FROM mastoorat_tashkeel WHERE groupId = :groupId")
    suspend fun deleteTashkeelOfGroup(groupId: Long)
}
