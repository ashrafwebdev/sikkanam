package com.ashrafwebdev.sikkanam.data

import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ashrafwebdev.sikkanam.R

/** How a trip is travelled. */
enum class TravelMode(@StringRes val labelRes: Int) {
    BUS(R.string.travel_mode_bus),
    TRAIN(R.string.travel_mode_train),
    FLIGHT(R.string.travel_mode_flight),
}

/**
 * A ticket booked by hand. Times are epoch millis. [arriveMillis] is 0 when unknown.
 * [groupId] is 0 when the booking is not linked to a jamaat trip group.
 */
@Entity(tableName = "travel_bookings")
data class TravelBooking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: TravelMode,
    val operator: String,
    val fromPlace: String,
    val toPlace: String,
    val departMillis: Long,
    val arriveMillis: Long,
    val reference: String,
    val fareCents: Long,
    val seat: String,
    val notes: String,
    val groupId: Long,
)
