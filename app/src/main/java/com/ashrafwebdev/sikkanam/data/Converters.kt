package com.ashrafwebdev.sikkanam.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun transactionTypeToString(type: TransactionType): String = type.name

    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun halalStatusToString(status: HalalStatus): String = status.name

    @TypeConverter
    fun stringToHalalStatus(value: String): HalalStatus = HalalStatus.valueOf(value)

    @TypeConverter
    fun donationKindToString(kind: DonationKind): String = kind.name

    @TypeConverter
    fun stringToDonationKind(value: String): DonationKind = DonationKind.valueOf(value)
}
