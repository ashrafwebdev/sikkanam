package com.ashrafwebdev.sikkanam.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun typeToString(type: TransactionType): String = type.name

    @TypeConverter
    fun stringToType(value: String): TransactionType = TransactionType.valueOf(value)
}
