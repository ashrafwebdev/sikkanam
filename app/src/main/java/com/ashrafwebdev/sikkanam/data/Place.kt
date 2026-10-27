package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Levels of the places hierarchy: India, then states, districts, zones, halqas and masjids.
 * Each place is one level below its parent. States have no parent, because India is the root.
 */
enum class PlaceLevel(val label: String, val childLabel: String?) {
    STATE("State", "District"),
    DISTRICT("District", "Zone"),
    ZONE("Zone", "Halqa"),
    HALQA("Halqa", "Masjid"),
    MASJID("Masjid", null),
}

/** One entry in the places hierarchy, entered by the user. Nothing here needs a permission. */
@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentId: Long?,
    val level: PlaceLevel,
    val name: String,
)
