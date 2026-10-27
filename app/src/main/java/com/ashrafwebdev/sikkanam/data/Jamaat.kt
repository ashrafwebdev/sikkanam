package com.ashrafwebdev.sikkanam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The muqami amaal, in the order they are shown. The key is stored; the label comes from the strings. */
enum class Amaal(val key: String) {
    MASJID_WARD_FIKR("masjid_ward_fikr"),
    TAALEEM_MASJID("taaleem_masjid"),
    TAALEEM_HOUSE("taaleem_house"),
    MUQAMI_GASHT("muqami_gasht"),
    SECOND_GASHT("second_gasht"),
    MASJID_ABAD("masjid_abad_ki_mehnat"),
    THREE_DAYS_JAMAAT("three_days_jamaat"),
}

/**
 * The weekly programmes of a jamaat. Attendance is recorded per week (the week's first day is the key day),
 * and the same AmaalCheck table stores it.
 */
enum class WeeklyProgramme(val key: String) {
    HALQA_MASHWARA("weekly_halqa_mashwara"),
    CITY_MASHWARA("weekly_city_mashwara"),
    SHABGUJARI("weekly_shabgujari"),
}

/** Whether one member did one amaal on one day. */
@Entity(tableName = "amaal_checks")
data class AmaalCheck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val memberId: Long,
    val dayMillis: Long,
    val amaal: String,
    val done: Boolean,
)

/** The kinds of khurooj and jamaat work recorded in the app. */
enum class KhuroojType(val label: String) {
    // Men
    MEN_40_DAYS_INDIA("Men: 40 days inside India"),
    MEN_4_MONTHS_INDIA("Men: 4 months inside India"),
    MEN_5_MONTHS_FOREIGN("Men: 5 months foreign"),
    // Ladies
    WOMEN_3_DAYS_INDIA("Ladies: 3 days inside India"),
    WOMEN_10_DAYS_INDIA("Ladies: 10 days inside India"),
    WOMEN_40_DAYS_INDIA("Ladies: 40 days inside India"),
    WOMEN_2_MONTHS_FOREIGN("Ladies: 2 months foreign"),
    WOMEN_2_MONTHS_NIZAMUDDIN("Ladies: 2 months Nizamuddin Markaz khidmat"),
    OTHER("Other"),
}

/** One khurooj of a jamaat: its type, dates, the places visited, and a note. [endMillis] is 0 while it is still going. */
@Entity(tableName = "khurooj")
data class Khurooj(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val type: KhuroojType = KhuroojType.OTHER,
    val startMillis: Long,
    val endMillis: Long = 0,
    val places: String = "",
    val note: String = "",
)
