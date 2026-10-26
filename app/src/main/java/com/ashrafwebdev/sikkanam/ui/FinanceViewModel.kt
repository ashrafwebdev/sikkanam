package com.ashrafwebdev.sikkanam.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashrafwebdev.sikkanam.data.AppDatabase
import com.ashrafwebdev.sikkanam.data.Budget
import com.ashrafwebdev.sikkanam.data.Donation
import com.ashrafwebdev.sikkanam.data.DonationKind
import com.ashrafwebdev.sikkanam.data.HalalStatus
import com.ashrafwebdev.sikkanam.data.Transaction
import com.ashrafwebdev.sikkanam.data.TransactionType
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

    fun shiftMonth(delta: Int) {
        _monthStart.value = addMonths(_monthStart.value, delta)
    }

    fun setQuery(text: String) {
        _query.value = text
    }

    fun setTypeFilter(type: TransactionType?) {
        _typeFilter.value = type
    }

    /** Saves a new transaction, or updates [existing] when it is not null. Returns false on invalid input. */
    fun saveTransaction(
        existing: Transaction?,
        type: TransactionType,
        amountText: String,
        category: String,
        note: String,
        halal: HalalStatus,
        isRiba: Boolean,
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
}
