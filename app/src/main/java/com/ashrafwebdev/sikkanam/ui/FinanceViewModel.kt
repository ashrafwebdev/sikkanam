package com.ashrafwebdev.sikkanam.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ashrafwebdev.sikkanam.data.Account
import com.ashrafwebdev.sikkanam.data.DutyAssignment
import com.ashrafwebdev.sikkanam.data.DutyType
import com.ashrafwebdev.sikkanam.data.Duties
import com.ashrafwebdev.sikkanam.data.Meeting
import com.ashrafwebdev.sikkanam.data.AppDatabase
import com.ashrafwebdev.sikkanam.data.Transfer
import com.ashrafwebdev.sikkanam.data.BackupData
import com.ashrafwebdev.sikkanam.data.Budget
import com.ashrafwebdev.sikkanam.data.Category
import com.ashrafwebdev.sikkanam.data.Donation
import com.ashrafwebdev.sikkanam.data.DonationKind
import com.ashrafwebdev.sikkanam.data.GroupKind
import com.ashrafwebdev.sikkanam.data.HalalStatus
import com.ashrafwebdev.sikkanam.data.GoalEntry
import com.ashrafwebdev.sikkanam.data.Place
import com.ashrafwebdev.sikkanam.network.refreshCuratedNetwork
import com.ashrafwebdev.sikkanam.data.SavingsGoal
import com.ashrafwebdev.sikkanam.data.Transaction
import com.ashrafwebdev.sikkanam.data.TripContribution
import com.ashrafwebdev.sikkanam.data.TripExpense
import com.ashrafwebdev.sikkanam.data.TripGroup
import com.ashrafwebdev.sikkanam.data.TripMember
import com.ashrafwebdev.sikkanam.data.TripSettledMonth
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

    val goals: StateFlow<List<SavingsGoal>> = db.savingsGoalDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goalEntries: StateFlow<List<GoalEntry>> = db.goalEntryDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val places: StateFlow<List<Place>> = db.placeDao().observeAll()
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
        generateRecurringTripExpenses()
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
        party: String,
        paymentMethod: String,
        accountId: Long? = null,
        receiptPath: String? = null,
    ): Boolean {
        val cents = parseCents(amountText) ?: return false
        if (category.isBlank() || party.isBlank() || note.isBlank() || paymentMethod.isBlank()) return false

        val entry = Transaction(
            id = existing?.id ?: 0,
            type = type,
            amountCents = cents,
            category = category.trim(),
            note = note.trim(),
            party = party.trim(),
            paymentMethod = paymentMethod.trim(),
            accountId = accountId,
            receiptPath = receiptPath,
            dateMillis = existing?.dateMillis ?: System.currentTimeMillis(),
            halalStatus = if (type == TransactionType.INCOME) halal else HalalStatus.NONE,
            isRiba = type == TransactionType.EXPENSE && isRiba,
            // Only templates repeat. Copies made from a template keep their link to it.
            recurringMonthly = existing?.sourceId == null && recurringMonthly,
            sourceId = existing?.sourceId,
        )
        // Replacing or removing a receipt leaves the old photo unused, so delete it.
        val oldReceipt = existing?.receiptPath
        if (oldReceipt != null && oldReceipt != receiptPath) deleteReceipt(getApplication<Application>(), oldReceipt)
        viewModelScope.launch {
            if (existing == null) db.transactionDao().insert(entry) else db.transactionDao().update(entry)
        }
        return true
    }

    fun deleteTransaction(id: Long) {
        transactions.value.firstOrNull { it.id == id }?.receiptPath?.let { deleteReceipt(getApplication<Application>(), it) }
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

    val tripGroups: StateFlow<List<TripGroup>> = db.tripDao().observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tripMembers: StateFlow<List<TripMember>> = db.tripDao().observeMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tripContributions: StateFlow<List<TripContribution>> = db.tripDao().observeContributions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tripExpenses: StateFlow<List<TripExpense>> = db.tripDao().observeExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTripGroup(name: String, kind: GroupKind = GroupKind.TRIP): Boolean {
        if (name.isBlank()) return false
        viewModelScope.launch { db.tripDao().insertGroup(TripGroup(name = name.trim(), kind = kind)) }
        return true
    }

    /** Deleting a trip removes its members, contributions, and expenses too. */
    fun deleteTripGroup(groupId: Long) {
        viewModelScope.launch {
            val tripDao = db.tripDao()
            tripContributions.value.filter { it.groupId == groupId }.forEach { tripDao.deleteContribution(it.id) }
            tripExpenses.value.filter { it.groupId == groupId }.forEach { tripDao.deleteExpense(it.id) }
            tripMembers.value.filter { it.groupId == groupId }.forEach { tripDao.deleteMember(it.id) }
            tripSettledMonths.value.filter { it.groupId == groupId }.forEach {
                tripDao.deleteSettledMonth(groupId, it.monthMillis)
            }
            tripDao.deleteGroup(groupId)
        }
    }

    fun addTripMember(groupId: Long, name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank()) return false
        if (tripMembers.value.any { it.groupId == groupId && it.name.equals(clean, ignoreCase = true) }) return false
        viewModelScope.launch { db.tripDao().insertMember(TripMember(groupId = groupId, name = clean)) }
        return true
    }

    /** A member with money or expenses on record can't be removed, so the totals stay correct. */
    fun deleteTripMember(member: TripMember): Boolean {
        val used = tripContributions.value.any { it.memberId == member.id } ||
            tripExpenses.value.any { it.paidByMemberId == member.id }
        if (used) return false
        viewModelScope.launch { db.tripDao().deleteMember(member.id) }
        return true
    }

    fun addTripContribution(groupId: Long, memberId: Long, amountText: String): Boolean {
        val cents = parseCents(amountText) ?: return false
        viewModelScope.launch {
            db.tripDao().insertContribution(
                TripContribution(groupId = groupId, memberId = memberId, amountCents = cents, dateMillis = System.currentTimeMillis())
            )
        }
        return true
    }

    fun deleteTripContribution(id: Long) {
        viewModelScope.launch { db.tripDao().deleteContribution(id) }
    }

    /**
     * [paidByMemberId] is null when the cost is paid from the shared pool.
     * [participantIds] are the members who share this cost. At least one is required.
     * [recurring] repeats the cost every month, from this month on.
     */
    fun addTripExpense(
        groupId: Long,
        paidByMemberId: Long?,
        description: String,
        amountText: String,
        participantIds: List<Long>,
        recurring: Boolean = false,
    ): Boolean {
        val cents = parseCents(amountText) ?: return false
        if (description.isBlank() || participantIds.isEmpty()) return false
        viewModelScope.launch {
            db.tripDao().insertExpense(
                TripExpense(
                    groupId = groupId,
                    paidByMemberId = paidByMemberId,
                    description = description.trim(),
                    amountCents = cents,
                    dateMillis = System.currentTimeMillis(),
                    participantIds = TripExpense.encode(participantIds),
                    recurringMonthly = recurring,
                )
            )
        }
        return true
    }

    val tripSettledMonths: StateFlow<List<TripSettledMonth>> = db.tripDao().observeSettledMonths()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Marks a month as settled, so balances after it start from zero. */
    fun markMonthSettled(groupId: Long, monthStart: Long) {
        viewModelScope.launch {
            db.tripDao().insertSettledMonth(TripSettledMonth(groupId = groupId, monthMillis = monthStart))
        }
    }

    fun reopenMonth(groupId: Long, monthStart: Long) {
        viewModelScope.launch { db.tripDao().deleteSettledMonth(groupId, monthStart) }
    }

    val accounts: StateFlow<List<Account>> = db.accountDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val transfers: StateFlow<List<Transfer>> = db.transferDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Current balance of each account: its opening balance, plus income, minus expenses, plus transfers in, minus transfers out. */
    val accountBalances: StateFlow<Map<Long, Long>> =
        combine(accounts, transactions, transfers) { accs, txs, moves ->
            accs.associate { account ->
                val income = txs.filter { it.accountId == account.id && it.type == TransactionType.INCOME }.sumOf { it.amountCents }
                val expense = txs.filter { it.accountId == account.id && it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
                val movedIn = moves.filter { it.toAccountId == account.id }.sumOf { it.amountCents }
                val movedOut = moves.filter { it.fromAccountId == account.id }.sumOf { it.amountCents }
                account.id to (account.openingCents + income - expense + movedIn - movedOut)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Adds an account. The opening balance may be zero, but not negative. */
    fun addAccount(name: String, openingText: String): Boolean {
        if (name.isBlank()) return false
        val opening = openingText.trim().toDoubleOrNull()?.takeIf { it >= 0 } ?: return false
        viewModelScope.launch {
            db.accountDao().insert(Account(name = name.trim(), openingCents = Math.round(opening * 100)))
        }
        return true
    }

    fun deleteAccount(id: Long) {
        viewModelScope.launch { db.accountDao().delete(id) }
    }

    /** Moves money between two different accounts. */
    fun addTransfer(fromId: Long, toId: Long, amountText: String, note: String): Boolean {
        val cents = parseCents(amountText) ?: return false
        if (fromId == toId) return false
        viewModelScope.launch {
            db.transferDao().insert(
                Transfer(
                    fromAccountId = fromId,
                    toAccountId = toId,
                    amountCents = cents,
                    dateMillis = System.currentTimeMillis(),
                    note = note.trim(),
                )
            )
        }
        return true
    }

    val meetings: StateFlow<List<Meeting>> = db.meetingDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adds a meeting, or updates [existing]. The topic is required. The date is kept when editing. */
    fun saveMeeting(existing: Meeting?, topic: String, attendees: String, notes: String): Boolean {
        val clean = topic.trim()
        if (clean.isBlank()) return false
        val entry = Meeting(
            id = existing?.id ?: 0,
            dateMillis = existing?.dateMillis ?: System.currentTimeMillis(),
            topic = clean,
            attendees = attendees.trim(),
            notes = notes.trim(),
        )
        viewModelScope.launch {
            if (existing == null) db.meetingDao().insert(entry) else db.meetingDao().update(entry)
        }
        return true
    }

    fun deleteMeeting(id: Long) {
        viewModelScope.launch { db.meetingDao().delete(id) }
    }

    val dutyAssignments: StateFlow<List<DutyAssignment>> = db.dutyDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dutyTypes: StateFlow<List<DutyType>> = db.dutyTypeDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adds a duty to the end of the list. Returns false if the name is blank, too long, or already listed. */
    fun addDutyType(name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank() || clean.length > 60) return false
        if (dutyTypes.value.any { it.name.equals(clean, ignoreCase = true) }) return false
        viewModelScope.launch { db.dutyTypeDao().insert(DutyType(clean, dutyTypes.value.size)) }
        return true
    }

    /** Renames a duty in the list and on every past assignment. */
    fun renameDutyType(oldName: String, newName: String): Boolean {
        val clean = newName.trim()
        if (clean.isBlank() || clean.length > 60) return false
        if (clean != oldName && dutyTypes.value.any { it.name.equals(clean, ignoreCase = true) }) return false
        val order = dutyTypes.value.firstOrNull { it.name == oldName }?.sortOrder ?: dutyTypes.value.size
        viewModelScope.launch {
            db.dutyTypeDao().delete(oldName)
            db.dutyTypeDao().insert(DutyType(clean, order))
            db.dutyDao().renameOnAssignments(oldName, clean)
        }
        return true
    }

    /** Removes a duty from the list. Past assignments of it are kept as history. */
    fun deleteDutyType(name: String) {
        viewModelScope.launch { db.dutyTypeDao().delete(name) }
    }

    /** Gives [duty] on [day] to [person], as decided in the mashwara. Replaces any earlier choice for that duty. */
    fun assignDuty(day: Long, duty: String, person: String) {
        viewModelScope.launch {
            db.dutyDao().deleteForDuty(day, duty)
            db.dutyDao().insert(DutyAssignment(dayMillis = day, duty = duty, personName = person))
        }
    }

    /** Removes the person given to [duty] on [day]. */
    fun clearDuty(day: Long, duty: String) {
        viewModelScope.launch { db.dutyDao().deleteForDuty(day, duty) }
    }

    fun setDutyDone(assignment: DutyAssignment, done: Boolean) {
        viewModelScope.launch { db.dutyDao().update(assignment.copy(done = done)) }
    }

    /** Edits a transfer. Same rules as adding one: two different accounts and an amount above zero. */
    fun updateTransfer(transfer: Transfer, fromId: Long, toId: Long, amountText: String, note: String): Boolean {
        val cents = parseCents(amountText) ?: return false
        if (fromId == toId) return false
        viewModelScope.launch {
            db.transferDao().update(
                transfer.copy(fromAccountId = fromId, toAccountId = toId, amountCents = cents, note = note.trim())
            )
        }
        return true
    }

    /** Edits an account's name and opening balance. */
    fun updateAccount(account: Account, name: String, openingText: String): Boolean {
        if (name.isBlank()) return false
        val opening = openingText.trim().toDoubleOrNull()?.takeIf { it >= 0 } ?: return false
        viewModelScope.launch {
            db.accountDao().update(account.copy(name = name.trim(), openingCents = Math.round(opening * 100)))
        }
        return true
    }

    fun updateGoal(goal: SavingsGoal, name: String, targetText: String): Boolean {
        val cents = parseCents(targetText) ?: return false
        if (name.isBlank()) return false
        viewModelScope.launch {
            db.savingsGoalDao().updateDetails(goal.id, name.trim(), cents)
        }
        return true
    }

    fun updateDonation(donation: Donation, kind: DonationKind, amountText: String, note: String): Boolean {
        val cents = parseCents(amountText) ?: return false
        viewModelScope.launch {
            db.donationDao().update(donation.copy(kind = kind, amountCents = cents, note = note.trim()))
        }
        return true
    }

    fun updateTripContribution(contribution: TripContribution, memberId: Long, amountText: String): Boolean {
        val cents = parseCents(amountText) ?: return false
        viewModelScope.launch {
            db.tripDao().updateContribution(contribution.copy(memberId = memberId, amountCents = cents))
        }
        return true
    }

    fun updateTripExpense(
        expense: TripExpense,
        paidByMemberId: Long?,
        description: String,
        amountText: String,
        participantIds: List<Long>,
    ): Boolean {
        val cents = parseCents(amountText) ?: return false
        if (description.isBlank() || participantIds.isEmpty()) return false
        viewModelScope.launch {
            db.tripDao().updateExpense(
                expense.copy(
                    paidByMemberId = paidByMemberId,
                    description = description.trim(),
                    amountCents = cents,
                    participantIds = TripExpense.encode(participantIds),
                )
            )
        }
        return true
    }

    fun renameTripGroup(group: TripGroup, name: String): Boolean {
        if (name.isBlank()) return false
        viewModelScope.launch { db.tripDao().updateGroup(group.copy(name = name.trim())) }
        return true
    }

    /**
     * Adds a member with registration details. Only the name is required.
     * [daysText] is the number of days, and [advanceText] is the advance paid. Both may be blank.
     */
    fun addTripMember(
        groupId: Long,
        name: String,
        phone: String,
        address: String,
        daysText: String,
        advanceText: String,
        confirmed: Boolean,
    ): Boolean {
        val clean = name.trim()
        if (clean.isBlank()) return false
        if (tripMembers.value.any { it.groupId == groupId && it.name.equals(clean, ignoreCase = true) }) return false
        val (days, advance) = parseMemberFields(daysText, advanceText) ?: return false
        viewModelScope.launch {
            db.tripDao().insertMember(
                TripMember(
                    groupId = groupId,
                    name = clean,
                    phone = phone.trim(),
                    address = address.trim(),
                    days = days,
                    advanceCents = advance,
                    confirmed = confirmed,
                )
            )
        }
        return true
    }

    /** Updates a member's name, going status, and registration details. */
    fun updateTripMember(
        member: TripMember,
        name: String,
        phone: String,
        address: String,
        daysText: String,
        advanceText: String,
        confirmed: Boolean,
    ): Boolean {
        val clean = name.trim()
        if (clean.isBlank()) return false
        if (tripMembers.value.any { it.groupId == member.groupId && it.id != member.id && it.name.equals(clean, ignoreCase = true) }) {
            return false
        }
        val (days, advance) = parseMemberFields(daysText, advanceText) ?: return false
        viewModelScope.launch {
            db.tripDao().updateMember(
                member.copy(
                    name = clean,
                    phone = phone.trim(),
                    address = address.trim(),
                    days = days,
                    advanceCents = advance,
                    confirmed = confirmed,
                )
            )
        }
        return true
    }

    /** Days must be zero or more. The advance may be blank (zero), but not negative. */
    private fun parseMemberFields(daysText: String, advanceText: String): Pair<Int, Long>? {
        val days = if (daysText.isBlank()) 0 else daysText.trim().toIntOrNull()?.takeIf { it >= 0 } ?: return null
        val advance = if (advanceText.isBlank()) 0L else {
            val amount = advanceText.trim().toDoubleOrNull()?.takeIf { it >= 0 } ?: return null
            Math.round(amount * 100)
        }
        return days to advance
    }

    fun renameTripMember(member: TripMember, name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank()) return false
        if (tripMembers.value.any { it.groupId == member.groupId && it.id != member.id && it.name.equals(clean, ignoreCase = true) }) {
            return false
        }
        viewModelScope.launch { db.tripDao().updateMember(member.copy(name = clean)) }
        return true
    }

    /**
     * Renames a category everywhere it's used: on transactions, budgets, and the master list.
     * Returns false if the new name is blank, too long, or already exists for that type.
     */
    fun renameCategory(oldName: String, newName: String, type: TransactionType): Boolean {
        val clean = newName.trim()
        if (clean.isBlank() || clean.length > 30) return false
        if (clean.equals(oldName, ignoreCase = false)) return true
        if (categories.value.any { it.type == type && it.name.equals(clean, ignoreCase = true) }) return false
        viewModelScope.launch {
            val renames = db.categoryRenameDao()
            db.categoryDao().delete(oldName, type)
            db.categoryDao().insert(Category(clean, type))
            renames.renameOnTransactions(oldName, clean, type)
            if (type == TransactionType.EXPENSE) renames.renameOnBudgets(oldName, clean)
        }
        return true
    }

    fun deleteTransfer(id: Long) {
        viewModelScope.launch { db.transferDao().delete(id) }
    }

    /** Switches a family group between equal shares and income-based shares. */
    fun setSplitByIncome(group: TripGroup, byIncome: Boolean) {
        viewModelScope.launch { db.tripDao().updateGroup(group.copy(splitByIncome = byIncome)) }
    }

    /** Sets a member's monthly income. Zero is allowed; a blank or negative amount is not. */
    fun setMemberIncome(member: TripMember, amountText: String): Boolean {
        val amount = amountText.trim().toDoubleOrNull() ?: return false
        if (amount < 0) return false
        val cents = Math.round(amount * 100)
        viewModelScope.launch { db.tripDao().updateMember(member.copy(incomeCents = cents)) }
        return true
    }

    /**
     * Adds a copy of each monthly family expense for every month since it started, up to this
     * month. Copies that already exist are skipped, so this is safe to run on every launch.
     */
    private fun generateRecurringTripExpenses() {
        viewModelScope.launch(Dispatchers.IO) {
            val tripDao = db.tripDao()
            val all = tripDao.getAllExpenses()
            val now = startOfMonth(System.currentTimeMillis())
            all.filter { it.recurringMonthly && it.sourceId == null }.forEach { template ->
                val templateMonth = startOfMonth(template.dateMillis)
                val dayOffset = template.dateMillis - templateMonth
                var month = addMonths(templateMonth, 1)
                while (month <= now) {
                    val exists = all.any { it.sourceId == template.id && startOfMonth(it.dateMillis) == month }
                    if (!exists) {
                        tripDao.insertExpense(
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

    fun deleteTripExpense(id: Long) {
        viewModelScope.launch { db.tripDao().deleteExpense(id) }
    }

    /** Adds a savings goal. Returns false if the name is blank or the target is not above zero. */
    fun saveGoal(name: String, targetText: String): Boolean {
        val cents = parseCents(targetText) ?: return false
        if (name.isBlank()) return false
        viewModelScope.launch {
            db.savingsGoalDao().insert(SavingsGoal(name = name.trim(), targetCents = cents, savedCents = 0))
        }
        return true
    }

    /** Puts money toward a goal and records it in the history. Returns false if the amount is not above zero. */
    fun addToGoal(goal: SavingsGoal, amountText: String, note: String): Boolean {
        val cents = parseCents(amountText) ?: return false
        viewModelScope.launch {
            db.goalEntryDao().insert(GoalEntry(goalId = goal.id, amountCents = cents, dateMillis = System.currentTimeMillis(), note = note.trim()))
            refreshSaved(goal.id)
        }
        return true
    }

    /** Corrects a mistaken amount or note in the history. Returns false if the amount is not above zero. */
    fun updateGoalEntry(entry: GoalEntry, amountText: String, note: String): Boolean {
        val cents = parseCents(amountText) ?: return false
        viewModelScope.launch {
            db.goalEntryDao().update(entry.copy(amountCents = cents, note = note.trim()))
            refreshSaved(entry.goalId)
        }
        return true
    }

    fun deleteGoalEntry(entry: GoalEntry) {
        viewModelScope.launch {
            db.goalEntryDao().delete(entry.id)
            refreshSaved(entry.goalId)
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            db.goalEntryDao().deleteForGoal(id)
            db.savingsGoalDao().delete(id)
        }
    }

    /** Downloads the curated network list. Returns an error message, or null on success. */
    suspend fun refreshNetwork(): String? = refreshCuratedNetwork(getApplication<Application>(), db.placeDao())

    /** Keeps a goal's saved total equal to the sum of its history. */
    private suspend fun refreshSaved(goalId: Long) {
        db.savingsGoalDao().setSaved(goalId, db.goalEntryDao().total(goalId))
    }

    /** The whole app's data as backup text. */
    fun backupText(): String = BackupData(
        transactions = transactions.value,
        budgets = budgets.value,
        donations = donations.value,
        categories = categories.value,
        goals = goals.value,
        tripGroups = tripGroups.value,
        tripMembers = tripMembers.value,
        tripContributions = tripContributions.value,
        tripExpenses = tripExpenses.value,
        tripSettledMonths = tripSettledMonths.value,
        accounts = accounts.value,
        transfers = transfers.value,
        meetings = meetings.value,
        dutyAssignments = dutyAssignments.value,
        dutyTypes = dutyTypes.value,
        goalEntries = goalEntries.value,
        places = places.value,
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
            data.goals.forEach { db.savingsGoalDao().insert(it) }
            data.goalEntries.forEach { db.goalEntryDao().insert(it) }
            data.places.forEach { db.placeDao().insert(it) }
            data.tripGroups.forEach { db.tripDao().insertGroup(it) }
            data.tripMembers.forEach { db.tripDao().insertMember(it) }
            data.tripContributions.forEach { db.tripDao().insertContribution(it) }
            data.tripExpenses.forEach { db.tripDao().insertExpense(it) }
            data.tripSettledMonths.forEach { db.tripDao().insertSettledMonth(it) }
            data.accounts.forEach { db.accountDao().insert(it) }
            data.transfers.forEach { db.transferDao().insert(it) }
            data.meetings.forEach { db.meetingDao().insert(it) }
            if (data.dutyAssignments.isNotEmpty()) db.dutyDao().insertAll(data.dutyAssignments)
            // Older backups have no duty list, so restore the defaults in that case.
            if (data.dutyTypes.isEmpty()) {
                Duties.defaults.forEachIndexed { index, name -> db.dutyTypeDao().insert(DutyType(name, index)) }
            } else {
                data.dutyTypes.forEach { db.dutyTypeDao().insert(it) }
            }
        }
        return true
    }
}
