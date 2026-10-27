package com.ashrafwebdev.sikkanam.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Transaction::class, Budget::class, Donation::class, Category::class, SavingsGoal::class,
        TripGroup::class, TripMember::class, TripContribution::class, TripExpense::class,
        TripSettledMonth::class, Account::class, Transfer::class,
    ],
    version = 15,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun donationDao(): DonationDao
    abstract fun categoryDao(): CategoryDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun tripDao(): TripDao
    abstract fun accountDao(): AccountDao
    abstract fun transferDao(): TransferDao
    abstract fun categoryRenameDao(): CategoryRenameDao

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

        /** Adds recurring-transaction columns. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurringMonthly INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN sourceId INTEGER")
            }
        }

        /** Adds the from/to-whom and payment-method columns. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN party TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Adds the savings goals table. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS savings_goals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, targetCents INTEGER NOT NULL, savedCents INTEGER NOT NULL)"
                )
            }
        }

        /** Adds the group trip tables. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS trip_groups (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS trip_members (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "groupId INTEGER NOT NULL, name TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS trip_contributions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "groupId INTEGER NOT NULL, memberId INTEGER NOT NULL, amountCents INTEGER NOT NULL, dateMillis INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS trip_expenses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "groupId INTEGER NOT NULL, paidByMemberId INTEGER, description TEXT NOT NULL, " +
                        "amountCents INTEGER NOT NULL, dateMillis INTEGER NOT NULL)"
                )
            }
        }

        /** Adds the list of people who share each trip expense. Old rows get '' (everyone). */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_expenses ADD COLUMN participantIds TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Marks each group as a trip or a family group. Existing groups become trips. */
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_groups ADD COLUMN kind TEXT NOT NULL DEFAULT 'TRIP'")
            }
        }

        /** Adds income-based splitting and recurring household expenses. */
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_groups ADD COLUMN splitByIncome INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trip_members ADD COLUMN incomeCents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trip_expenses ADD COLUMN recurringMonthly INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trip_expenses ADD COLUMN sourceId INTEGER")
            }
        }

        /** Adds the settled-month record used for carrying balances forward. */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS trip_settled_months (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "groupId INTEGER NOT NULL, monthMillis INTEGER NOT NULL)"
                )
            }
        }

        /** Adds accounts, transfers, and the account link on each transaction. */
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, openingCents INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS transfers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "fromAccountId INTEGER NOT NULL, toAccountId INTEGER NOT NULL, amountCents INTEGER NOT NULL, " +
                        "dateMillis INTEGER NOT NULL, note TEXT NOT NULL)"
                )
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER")
            }
        }

        /** Adds the newer default categories to existing installs. Existing categories are kept. */
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                DefaultCategories.insertInto { sql, args -> db.execSQL(sql, args) }
            }
        }

        /** Adds the default Cash and Bank accounts to existing installs. */
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                DefaultCategories.insertAccountsInto { sql, args -> db.execSQL(sql, args) }
            }
        }

        /** Adds the receipt file name to each transaction. */
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN receiptPath TEXT")
            }
        }

        /** Seeds the default categories on a brand-new install, which skips the migrations. */
        private val SEED_ON_CREATE = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                DefaultCategories.insertInto { sql, args -> db.execSQL(sql, args) }
                DefaultCategories.insertAccountsInto { sql, args -> db.execSQL(sql, args) }
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "sikkanam.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                    MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
                    MIGRATION_14_15,
                )
                .addCallback(SEED_ON_CREATE)
                .build()
    }
}
