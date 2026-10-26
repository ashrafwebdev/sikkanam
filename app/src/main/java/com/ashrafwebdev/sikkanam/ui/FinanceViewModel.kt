package com.ashrafwebdev.sikkanam.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashrafwebdev.sikkanam.data.AppDatabase
import com.ashrafwebdev.sikkanam.data.BackupData
import com.ashrafwebdev.sikkanam.data.Budget
import com.ashrafwebdev.sikkanam.data.Category
import com.ashrafwebdev.sikkanam.data.Donation
import com.ashrafwebdev.sikkanam.data.DonationKind
import com.ashrafwebdev.sikkanam.data.HalalStatus
import com.ashrafwebdev.sikkanam.data.Transaction
import com.ashrafwebdev.sikkanam.data.TransactionType
import com.ashrafwebdev.sikkanam.data.parseBackup
import com.ashrafwebdev.sikkanam.data.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.create(application)

    val transactions: StateFlow<List<Transaction>> = db.transactionDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budgets: StateFlow<List<Budget>> = db.budgetDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val donations: StateFlow<List<Donation>> = db.donationDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The master category list, for both income and expense. */
    val categories: StateFlow<List<Category>> = db.categoryDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Start of the month being viewed, as epoch millis in local time. */
    private val _monthStart = MutableStateFlow(startOfMonth(System.currentTimeMillis()))
    val monthStart: StateFlow<Long> = _monthStart

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _typeFilter = MutableStateFlow<TransactionType?>(null)
    val typeFilter: StateFlow<TransactionType?> = _typeFilter

    /** Transactions in the viewed month that match the search text and type filter. */
    val visibleTransactions: StateFlow<List<Transaction>> =
        combine(transactions, _monthStart, _query, _typeFilter) { all, start, query, type ->
            val end = addMonths(start, 1)
            all.filter { t ->
                t.dateMillis >= start && t.dateMillis < end &&
                    (type == null || t.type == type) &&
                    (query.isBlank() ||
                        t.category.contains(query, ignoreCase = true) ||
                        t.note.contains(query, ignoreCase = true))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All transactions in the viewed month, ignoring search and filter. Used for totals and budgets. */
    val monthTransactions: StateFlow<List<Transaction>> =
        combine(transactions, _monthStart) { all, start ->
            val end = addMonths(start, 1)
            all.filter { it.dateMillis >= start && it.dateMillis < end }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        generateRecurringTransactions()
    }

    /**
     * Adds a copy of each monthly template for every month since it started, up to this month.
     * Copies that already exist are skipped, so this is safe to run on every launch.
     */
    private fun generateRecurringTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = db.transactionDao().getAll()
            val now = startOfMonth(System.currentTimeMillis())
            all.filter { it.recurringMonthly && it.sourceId == null }.forEach { template ->
                val templateMonth = startOfMonth(template.dateMillis)
                val dayOffset = template.dateMillis - templateMonth
                var month = addMonths(templateMonth, 1)
                while (month <= now) {
                    val exists = all.any { it.sourceId == template.id && startOfMonth(it.dateMillis) == month }
                    if (!exists) {
                        db.transactionDao().insert(
                            template.copy(
                                id = 0,
                                dateMillis = month + dayOffset,
                                recurringMonthly = false,
                                sourceId = template.id,
                            )
                        )
                    }
                    month = addMonths(month, 1)
                }
            }
        }
    }

    fun shiftMonth(delta: Int) {
        _monthStart.value = addMonths(_monthStart.value, delta)
    }

    fun setQuery(text: String) {
        _query.value = text
    }

    fun setTypeFilter(type: TransactionType?) {
        _typeFilter.value = type
    }

    /**
     * Saves a new transaction, or updates [existing] when it is not null.
     * Returns false on invalid input, so the dialog can show a message and stay open.
     */
    fun saveTransaction(
        existing: Transaction?,
        type: TransactionType,
        amountText: String,
        category: String,
        note: String,
        halal: HalalStatus,
        isRiba: Boolean,
        recurringMonthly: Boolean,
    ): Boolean {
        val cents = parseCents(amountText) ?: return false
        if (category.isBlank()) return false

        val entry = Transaction(
            id = existing?.id ?: 0,
            type = type,
            amountCents = cents,
            category = category.trim(),
            note = note.trim(),
            dateMillis = existing?.dateMillis ?: System.currentTimeMillis(),
            halalStatus = if (type == TransactionType.INCOME) halal else HalalStatus.NONE,
            isRiba = type == TransactionType.EXPENSE && isRiba,
            // Only templates repeat. Copies made from a template keep their link to it.
            recurringMonthly = existing?.sourceId == null && recurringMonthly,
            sourceId = existing?.sourceId,
        )
        viewModelScope.launch {
            if (existing == null) db.transactionDao().insert(entry) else db.transactionDao().update(entry)
        }
        return true
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { db.transactionDao().delete(id) }
    }

    fun saveBudget(category: String, limitText: String): Boolean {
        val cents = parseCents(limitText) ?: return false
        if (category.isBlank()) return false
        viewModelScope.launch {
            db.budgetDao().save(Budget(category.trim(), cents))
        }
        return true
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch { db.budgetDao().delete(category) }
    }

    fun saveDonation(kind: DonationKind, amountText: String, note: String): Boolean {
        val cents = parseCents(amountText) ?: return false
        viewModelScope.launch {
            db.donationDao().insert(
                Donation(kind = kind, amountCents = cents, note = note.trim(), dateMillis = System.currentTimeMillis())
            )
        }
        return true
    }

    fun deleteDonation(id: Long) {
        viewModelScope.launch { db.donationDao().delete(id) }
    }

    /** Adds a category to the master list. Returns false if the name is blank, too long, or already exists. */
    fun addCategory(name: String, type: TransactionType): Boolean {
        val clean = name.trim()
        if (clean.isBlank() || clean.length > 30) return false
        val exists = categories.value.any { it.type == type && it.name.equals(clean, ignoreCase = true) }
        if (exists) return false
        viewModelScope.launch { db.categoryDao().insert(Category(clean, type)) }
        return true
    }

    fun deleteCategory(name: String, type: TransactionType) {
        viewModelScope.launch { db.categoryDao().delete(name, type) }
    }

    /** The whole app's data as backup text. */
    fun backupText(): String = BackupData(
        transactions = transactions.value,
        budgets = budgets.value,
        donations = donations.value,
        categories = categories.value,
    ).toJson()

    /**
     * Replaces all data with the contents of a backup. Returns false if the text isn't a valid backup.
     * The old data is removed first, so restoring twice gives the same result.
     */
    fun restoreBackup(text: String): Boolean {
        val data = try {
            parseBackup(text)
        } catch (e: Exception) {
            return false
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.clearAllTables()
            data.categories.forEach { db.categoryDao().insert(it) }
            data.transactions.forEach { db.transactionDao().insert(it) }
            data.budgets.forEach { db.budgetDao().save(it) }
            data.donations.forEach { db.donationDao().insert(it) }
        }
        return true
    }
}
