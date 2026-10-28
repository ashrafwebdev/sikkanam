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

    @TypeConverter
    fun groupKindToString(kind: GroupKind): String = kind.name

    @TypeConverter
    fun stringToGroupKind(value: String): GroupKind = GroupKind.valueOf(value)

    @TypeConverter
    fun mastooratEffortToString(effort: MastooratEffort): String = effort.name

    @TypeConverter
    fun stringToMastooratEffort(value: String): MastooratEffort = MastooratEffort.valueOf(value)

    @TypeConverter
    fun taleemKindToString(kind: TaleemKind): String = kind.name

    @TypeConverter
    fun stringToTaleemKind(value: String): TaleemKind = TaleemKind.valueOf(value)

    @TypeConverter
    fun mahramRelationToString(relation: MahramRelation): String = relation.name

    @TypeConverter
    fun stringToMahramRelation(value: String): MahramRelation = MahramRelation.valueOf(value)

    @TypeConverter
    fun tashkeelKindToString(kind: TashkeelKind): String = kind.name

    @TypeConverter
    fun stringToTashkeelKind(value: String): TashkeelKind = TashkeelKind.valueOf(value)
}
