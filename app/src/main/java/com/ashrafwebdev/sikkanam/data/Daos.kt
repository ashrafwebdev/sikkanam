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
interface DonationDao {
    @Query("SELECT * FROM donations ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<Donation>>

    @Insert
    suspend fun insert(donation: Donation)

    @Query("DELETE FROM donations WHERE id = :id")
    suspend fun delete(id: Long)
}
