package com.ashrafwebdev.sikkanam.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * A person in the master register. Dawat, locality and jamaat records link to one person, so a person
 * who appears in more than one section is stored once. The name and phone on those records are copies
 * taken when they were linked.
 */
@Entity(tableName = "people")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
)

/**
 * The person chosen in a form. [personId] is the saved person picked from the list, or 0 when the
 * person is new and will be saved to the list when the form is saved.
 */
data class PersonChoice(
    val personId: Long = 0,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
)

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Person>>

    @Query("SELECT * FROM people")
    suspend fun all(): List<Person>

    @Insert
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)
}
