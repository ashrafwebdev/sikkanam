package com.ashrafwebdev.sikkanam.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions")
    suspend fun getAll(): List<Transaction>

    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name")
    fun observeAll(): Flow<List<Account>>

    @Insert
    suspend fun insert(account: Account)

    @Update
    suspend fun update(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<Transfer>>

    @Insert
    suspend fun insert(transfer: Transfer)

    @Update
    suspend fun update(transfer: Transfer)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface DutyDao {
    @Query("SELECT * FROM duty_assignments ORDER BY dayMillis, id")
    fun observeAll(): Flow<List<DutyAssignment>>

    @Insert
    suspend fun insertAll(assignments: List<DutyAssignment>)

    @Update
    suspend fun update(assignment: DutyAssignment)

    @Query("DELETE FROM duty_assignments")
    suspend fun deleteAll()

    @Query("DELETE FROM duty_assignments WHERE dayMillis = :day AND duty = :duty")
    suspend fun deleteForDuty(day: Long, duty: String)

    /** Renames a duty on every past assignment, so the history follows the new name. */
    @Query("UPDATE duty_assignments SET duty = :newName WHERE duty = :oldName")
    suspend fun renameOnAssignments(oldName: String, newName: String)

    @Insert
    suspend fun insert(assignment: DutyAssignment)
}

@Dao
interface DutyTypeDao {
    @Query("SELECT * FROM duty_types ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<DutyType>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(duty: DutyType)

    @Query("DELETE FROM duty_types WHERE name = :name")
    suspend fun delete(name: String)
}

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meetings ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<Meeting>>

    @Insert
    suspend fun insert(meeting: Meeting)

    @Update
    suspend fun update(meeting: Meeting)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CategoryRenameDao {
    /** Renames a category on every transaction and budget that uses it. */
    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName AND type = :type")
    suspend fun renameOnTransactions(oldName: String, newName: String, type: TransactionType)

    @Query("UPDATE budgets SET category = :newName WHERE category = :oldName")
    suspend fun renameOnBudgets(oldName: String, newName: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY category")
    fun observeAll(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(budget: Budget)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun delete(category: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category)

    @Query("DELETE FROM categories WHERE name = :name AND type = :type")
    suspend fun delete(name: String, type: TransactionType)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY name")
    fun observeAll(): Flow<List<SavingsGoal>>

    @Insert
    suspend fun insert(goal: SavingsGoal)

    @Update
    suspend fun update(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun delete(id: Long)

    /** Changes only the name and target, so the saved total is never overwritten from a stale copy. */
    @Query("UPDATE savings_goals SET name = :name, targetCents = :targetCents WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, targetCents: Long)

    @Query("UPDATE savings_goals SET savedCents = :savedCents WHERE id = :id")
    suspend fun setSaved(id: Long, savedCents: Long)
}

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY id")
    fun observeAll(): Flow<List<Place>>

    /** Returns the new row's id, so child places can point to their parent. */
    @Insert
    suspend fun insert(place: Place): Long

    @Query("DELETE FROM places")
    suspend fun clearAll()
}

@Dao
interface GoalEntryDao {
    @Query("SELECT * FROM goal_entries ORDER BY dateMillis DESC, id DESC")
    fun observeAll(): Flow<List<GoalEntry>>

    @Insert
    suspend fun insert(entry: GoalEntry)

    @Update
    suspend fun update(entry: GoalEntry)

    @Query("DELETE FROM goal_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM goal_entries WHERE goalId = :goalId")
    suspend fun deleteForGoal(goalId: Long)

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM goal_entries WHERE goalId = :goalId")
    suspend fun total(goalId: Long): Long
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trip_groups ORDER BY id")
    fun observeGroups(): Flow<List<TripGroup>>

    @Query("SELECT * FROM trip_members ORDER BY id")
    fun observeMembers(): Flow<List<TripMember>>

    @Query("SELECT * FROM trip_contributions ORDER BY dateMillis DESC")
    fun observeContributions(): Flow<List<TripContribution>>

    @Query("SELECT * FROM trip_expenses ORDER BY dateMillis DESC")
    fun observeExpenses(): Flow<List<TripExpense>>

    @Insert
    suspend fun insertGroup(group: TripGroup): Long

    @Query("DELETE FROM trip_groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    @Update
    suspend fun updateGroup(group: TripGroup)

    @Update
    suspend fun updateMember(member: TripMember)

    @Insert
    suspend fun insertMember(member: TripMember)

    @Query("DELETE FROM trip_members WHERE id = :id")
    suspend fun deleteMember(id: Long)

    @Update
    suspend fun updateContribution(contribution: TripContribution)

    @Update
    suspend fun updateExpense(expense: TripExpense)

    @Insert
    suspend fun insertContribution(contribution: TripContribution)

    @Query("DELETE FROM trip_contributions WHERE id = :id")
    suspend fun deleteContribution(id: Long)

    @Query("SELECT * FROM trip_expenses")
    suspend fun getAllExpenses(): List<TripExpense>

    @Query("SELECT * FROM trip_settled_months")
    fun observeSettledMonths(): Flow<List<TripSettledMonth>>

    @Insert
    suspend fun insertSettledMonth(month: TripSettledMonth)

    @Query("DELETE FROM trip_settled_months WHERE groupId = :groupId AND monthMillis = :monthMillis")
    suspend fun deleteSettledMonth(groupId: Long, monthMillis: Long)

    @Insert
    suspend fun insertExpense(expense: TripExpense)

    @Query("DELETE FROM trip_expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)
}

@Dao
interface DonationDao {
    @Query("SELECT * FROM donations ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<Donation>>

    @Insert
    suspend fun insert(donation: Donation)

    @Update
    suspend fun update(donation: Donation)

    @Query("DELETE FROM donations WHERE id = :id")
    suspend fun delete(id: Long)
}
