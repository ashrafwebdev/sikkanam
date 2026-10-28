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
        TripSettledMonth::class, Account::class, Transfer::class, Meeting::class, DutyAssignment::class, DutyType::class,
        GoalEntry::class, Place::class, Debt::class, AmaalCheck::class, Khurooj::class, DawatContact::class, LocalityPerson::class, MeetingVisit::class,
        Note::class, TravelBooking::class,
    ],
    version = 32,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun donationDao(): DonationDao
    abstract fun categoryDao(): CategoryDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun goalEntryDao(): GoalEntryDao
    abstract fun placeDao(): PlaceDao
    abstract fun debtDao(): DebtDao
    abstract fun amaalDao(): AmaalDao
    abstract fun khuroojDao(): KhuroojDao
    abstract fun dawatDao(): DawatDao
    abstract fun localityDao(): LocalityDao
    abstract fun meetingVisitDao(): MeetingVisitDao
    abstract fun tripDao(): TripDao
    abstract fun accountDao(): AccountDao
    abstract fun transferDao(): TransferDao
    abstract fun categoryRenameDao(): CategoryRenameDao
    abstract fun meetingDao(): MeetingDao
    abstract fun dutyDao(): DutyDao
    abstract fun dutyTypeDao(): DutyTypeDao
    abstract fun noteDao(): NoteDao
    abstract fun travelDao(): TravelDao

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

        /** Adds registration details (phone, address, days, advance) to trip members. */
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_members ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trip_members ADD COLUMN address TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE trip_members ADD COLUMN days INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trip_members ADD COLUMN advanceCents INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds the going/interested status. Existing members count as going, as before. */
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_members ADD COLUMN confirmed INTEGER NOT NULL DEFAULT 1")
            }
        }

        /** Adds the daily meetings (mashwara) table. */
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS meetings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "dateMillis INTEGER NOT NULL, topic TEXT NOT NULL, attendees TEXT NOT NULL, notes TEXT NOT NULL)"
                )
            }
        }

        /** Adds the duty rotation table. */
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS duty_assignments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "dayMillis INTEGER NOT NULL, duty TEXT NOT NULL, personName TEXT NOT NULL, done INTEGER NOT NULL)"
                )
            }
        }

        /** Adds the editable duty list, seeded with the default duties. */
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS duty_types (name TEXT NOT NULL PRIMARY KEY, sortOrder INTEGER NOT NULL)")
                Duties.insertDefaults { sql, args -> db.execSQL(sql, args) }
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
        /** Adds a history of goal entries. Money already saved before this is kept as one opening entry per goal. */
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS goal_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "goalId INTEGER NOT NULL, amountCents INTEGER NOT NULL, dateMillis INTEGER NOT NULL, " +
                        "note TEXT NOT NULL DEFAULT '')"
                )
                db.execSQL(
                    "INSERT INTO goal_entries (goalId, amountCents, dateMillis, note) " +
                        "SELECT id, savedCents, CAST(strftime('%s','now') AS INTEGER) * 1000, 'Saved before history was added' " +
                        "FROM savings_goals WHERE savedCents > 0"
                )
            }
        }

        /** Adds the places hierarchy (states, districts, zones, halqas, masjids). */
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS places (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "parentId INTEGER, level TEXT NOT NULL, name TEXT NOT NULL)"
                )
            }
        }

        /** Adds the debts table (money lent and borrowed). */
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS debts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "person TEXT NOT NULL, direction TEXT NOT NULL, amountCents INTEGER NOT NULL, " +
                        "paidCents INTEGER NOT NULL, dateMillis INTEGER NOT NULL, dueMillis INTEGER NOT NULL, " +
                        "note TEXT NOT NULL)"
                )
            }
        }

        /** Links meetings and duty assignments to a jamaat. Older rows get 0 (not linked). */
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meetings ADD COLUMN groupId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE duty_assignments ADD COLUMN groupId INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds the muqami amaal checks and the khurooj records. */
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS amaal_checks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "groupId INTEGER NOT NULL, memberId INTEGER NOT NULL, dayMillis INTEGER NOT NULL, " +
                        "amaal TEXT NOT NULL, done INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS khurooj (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "groupId INTEGER NOT NULL, type TEXT NOT NULL DEFAULT 'OTHER', startMillis INTEGER NOT NULL, " +
                        "endMillis INTEGER NOT NULL, places TEXT NOT NULL, note TEXT NOT NULL)"
                )
            }
        }

        /** Adds the dawat contacts (people met, taskeel status, follow-up dates). */
        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS dawat_contacts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, phone TEXT NOT NULL, area TEXT NOT NULL, status TEXT NOT NULL, " +
                        "metMillis INTEGER NOT NULL, followUpMillis INTEGER NOT NULL, notes TEXT NOT NULL)"
                )
            }
        }

        /** Adds the locality register (people in the masjid's area, and their khurooj history). */
        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS locality_people (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, phone TEXT NOT NULL, locality TEXT NOT NULL, " +
                        "wentFortyDays INTEGER NOT NULL, wentFourMonths INTEGER NOT NULL, active INTEGER NOT NULL, " +
                        "notes TEXT NOT NULL)"
                )
            }
        }

        /** Adds the visit log, so each meeting with a person from the register is counted. */
        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS meeting_visits (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "personId INTEGER NOT NULL, dayMillis INTEGER NOT NULL, note TEXT NOT NULL)"
                )
            }
        }

        /** Adds the khurooj types each person is available for, and the taskeel given at each visit. */
        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE locality_people ADD COLUMN availableTypes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE meeting_visits ADD COLUMN taskeel TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Adds the personal notes table (title and text). */
        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, body TEXT NOT NULL, updatedMillis INTEGER NOT NULL)"
                )
            }
        }

        /** Adds the travel bookings table (bus, train and flight tickets recorded by hand). */
        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS travel_bookings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "mode TEXT NOT NULL, operator TEXT NOT NULL, fromPlace TEXT NOT NULL, toPlace TEXT NOT NULL, " +
                        "departMillis INTEGER NOT NULL, arriveMillis INTEGER NOT NULL, reference TEXT NOT NULL, " +
                        "fareCents INTEGER NOT NULL, seat TEXT NOT NULL, notes TEXT NOT NULL, groupId INTEGER NOT NULL)"
                )
            }
        }

        /** Adds the pinned flag to notes, so pinned notes can be listed first. */
        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val SEED_ON_CREATE = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                DefaultCategories.insertInto { sql, args -> db.execSQL(sql, args) }
                DefaultCategories.insertAccountsInto { sql, args -> db.execSQL(sql, args) }
                Duties.insertDefaults { sql, args -> db.execSQL(sql, args) }
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "sikkanam.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                    MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
                    MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
                    MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30,
                    MIGRATION_30_31, MIGRATION_31_32,
                )
                .addCallback(SEED_ON_CREATE)
                .build()
    }
}
