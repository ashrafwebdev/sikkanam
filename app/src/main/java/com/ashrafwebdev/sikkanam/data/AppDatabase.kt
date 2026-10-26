package com.ashrafwebdev.sikkanam.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, Budget::class, Donation::class, Category::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun donationDao(): DonationDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN halalStatus TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isRiba INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS budgets (category TEXT NOT NULL PRIMARY KEY, monthlyLimitCents INTEGER NOT NULL)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS donations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "kind TEXT NOT NULL, amountCents INTEGER NOT NULL, note TEXT NOT NULL, dateMillis INTEGER NOT NULL)"
                )
            }
        }

        /** Creates the master category list and seeds it with the standard categories. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories (name TEXT NOT NULL, type TEXT NOT NULL, " +
                        "PRIMARY KEY(name, type))"
                )
                val expense = listOf("Food", "Transport", "Bills", "Rent", "Shopping", "Health", "Education", "Family", "Charity", "Other")
                val income = listOf("Salary", "Business", "Gifts", "Other")
                expense.forEach { db.execSQL("INSERT OR IGNORE INTO categories (name, type) VALUES ('$it', 'EXPENSE')") }
                income.forEach { db.execSQL("INSERT OR IGNORE INTO categories (name, type) VALUES ('$it', 'INCOME')") }
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "sikkanam.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
