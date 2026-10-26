package com.ashrafwebdev.sikkanam.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashrafwebdev.sikkanam.data.AppDatabase
import com.ashrafwebdev.sikkanam.data.Transaction
import com.ashrafwebdev.sikkanam.data.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.create(application).transactionDao()

    val transactions: StateFlow<List<Transaction>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Returns false when the input is invalid, so the dialog can stay open. */
    fun add(type: TransactionType, amountText: String, category: String, note: String): Boolean {
        val cents = amountText.trim().toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: return false
        if (cents <= 0 || category.isBlank()) return false

        viewModelScope.launch {
            dao.insert(
                Transaction(
                    type = type,
                    amountCents = cents,
                    category = category.trim(),
                    note = note.trim(),
                    dateMillis = System.currentTimeMillis(),
                )
            )
        }
        return true
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }
}
