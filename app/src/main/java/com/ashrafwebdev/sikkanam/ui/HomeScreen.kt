package com.ashrafwebdev.sikkanam.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.StringRes
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.BuildConfig
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.Account
import com.ashrafwebdev.sikkanam.data.Budget
import com.ashrafwebdev.sikkanam.data.Category
import com.ashrafwebdev.sikkanam.data.HalalStatus
import com.ashrafwebdev.sikkanam.data.Transaction
import com.ashrafwebdev.sikkanam.data.TransactionType
import com.ashrafwebdev.sikkanam.payments.IncomingShare
import com.ashrafwebdev.sikkanam.payments.SharedReceipt
import java.util.Calendar
import kotlin.math.roundToInt

/** The sections of the Money page, shown as chips at the top. */
private enum class MoneySection(@StringRes val labelRes: Int) {
    SUMMARY(R.string.section_summary),
    ACCOUNTS(R.string.section_accounts),
    TRANSACTIONS(R.string.section_transactions),
}

@Composable
fun HomeScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                writer.write(transactionsToCsv(vm.transactions.value))
            }
        }
    }

    val exportFileName = stringResource(R.string.home_export_file)
    val monthStart by vm.monthStart.collectAsState()
    val monthTx by vm.monthTransactions.collectAsState()
    val allTx by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val visible by vm.visibleTransactions.collectAsState()
    val query by vm.query.collectAsState()
    val typeFilter by vm.typeFilter.collectAsState()
    val budgets by vm.budgets.collectAsState()

    var section by rememberSaveable { mutableStateOf(MoneySection.SUMMARY) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val income = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
    val expense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
    val interest = monthTx.filter { it.isRiba }.sumOf { it.amountCents }

    // Starts from every account's opening balance, then adds every transaction up to the end of the
    // viewed month, so the balance carries forward. Transfers between accounts net to zero, so they are left out.
    val monthEnd = addMonths(monthStart, 1)
    val balanceToDate = accounts.sumOf { it.openingCents } + allTx.filter { it.dateMillis < monthEnd }.sumOf {
        if (it.type == TransactionType.INCOME) it.amountCents else -it.amountCents
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionChips(
            options = MoneySection.entries,
            selected = section,
            label = { stringResource(it.labelRes) },
            onSelect = { section = it },
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(stringResource(R.string.home_hijri_date, hijriToday()), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.shiftMonth(-1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = stringResource(R.string.home_previous_month))
                    }
                    Text(
                        monthLabel(monthStart),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.shiftMonth(1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = stringResource(R.string.home_next_month))
                    }
                }
            }

            if (section == MoneySection.SUMMARY) {
                item { MonthlySummaryCard(vm) }
                item { BackupReminderCard() }
                if (BuildConfig.PAYMENT_ALERTS) item { PendingPaymentsCard(vm) }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                stringResource(R.string.home_balance),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                formatMoney(balanceToDate),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    stringResource(R.string.home_money_in, formatMoney(income)),
                                    color = IncomeColor,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    stringResource(R.string.home_money_out, formatMoney(expense)),
                                    color = ExpenseColor,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (interest > 0) {
                                Text(stringResource(R.string.home_interest_paid, formatMoney(interest)))
                            }
                        }
                    }
                }

                item {
                    CategoryBreakdown(
                        monthTx = monthTx,
                        budgets = budgets,
                        onOpenTransaction = { editing = it; showDialog = true },
                    )
                }

                item {
                    YearSummary(allTx, monthStart)
                }
            }

            if (section == MoneySection.ACCOUNTS) {
                item {
                    AccountsCard(vm)
                }
            }

            if (section == MoneySection.TRANSACTIONS) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        FilledTonalIconButton(onClick = { editing = null; showDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
                        }
                        TextButton(onClick = { exportLauncher.launch(exportFileName) }) {
                            Text(stringResource(R.string.home_export_csv))
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = vm::setQuery,
                        label = { Text(stringResource(R.string.home_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppChip(
                            selected = typeFilter == null,
                            onClick = { vm.setTypeFilter(null) },
                            label = { Text(stringResource(R.string.filter_all)) },
                        )
                        AppChip(
                            selected = typeFilter == TransactionType.INCOME,
                            onClick = { vm.setTypeFilter(TransactionType.INCOME) },
                            label = { Text(stringResource(R.string.type_income)) },
                        )
                        AppChip(
                            selected = typeFilter == TransactionType.EXPENSE,
                            onClick = { vm.setTypeFilter(TransactionType.EXPENSE) },
                            label = { Text(stringResource(R.string.type_expense)) },
                        )
                    }
                }

                items(visible, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { editing = transaction; showDialog = true },
                        onDelete = { vm.deleteTransaction(transaction.id) },
                    )
                }
            }
        }
    }

    val share by IncomingShare.pending.collectAsState()
    share?.let { shared ->
        TransactionDialog(
            existing = null,
            shared = shared,
            categories = categories,
            allTransactions = allTx,
            onDismiss = {
                // The copied receipt is not needed if the entry was never saved.
                shared.receiptName?.let { deleteReceipt(context, it) }
                IncomingShare.clear()
            },
            accounts = accounts,
            onSave = { type, amount, category, party, note, method, halal, riba, recurring, accountId, receipt ->
                vm.saveTransaction(
                    null, type, amount, category, note, halal, riba, recurring, party, method, accountId, receipt,
                ).also { saved ->
                    if (saved) {
                        if (shared.receiptName != null && shared.receiptName != receipt) deleteReceipt(context, shared.receiptName)
                        IncomingShare.clear()
                    }
                }
            },
        )
    }

    if (showDialog) {
        TransactionDialog(
            existing = editing,
            categories = categories,
            allTransactions = allTx,
            onDismiss = { showDialog = false },
            accounts = accounts,
            onSave = { type, amount, category, party, note, method, halal, riba, recurring, accountId, receipt ->
                vm.saveTransaction(
                    editing, type, amount, category, note, halal, riba, recurring, party, method, accountId, receipt,
                ).also { saved ->
                    if (saved) showDialog = false
                }
            },
        )
    }
}

/** Expense per category this month. Tapping a category opens its details. */
@Composable
private fun CategoryBreakdown(monthTx: List<Transaction>, budgets: List<Budget>, onOpenTransaction: (Transaction) -> Unit) {
    val expenses = monthTx.filter { it.type == TransactionType.EXPENSE }
    val byCategory = expenses
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amountCents } }
        .entries
        .sortedByDescending { it.value }
    if (byCategory.isEmpty()) return
    var opened by remember { mutableStateOf<String?>(null) }

    val total = byCategory.sumOf { it.value }.toFloat()
    SectionCard {
        Text(stringResource(R.string.home_spending_by_category), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.home_category_tap_hint), style = MaterialTheme.typography.bodySmall)
        byCategory.forEach { (category, cents) ->
            val fraction = cents / total
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { opened = category }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$category: ${formatMoney(cents)}", modifier = Modifier.weight(1f))
                    Text("${(fraction * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    opened?.let { category ->
        CategoryDetailsDialog(
            category = category,
            entries = expenses.filter { it.category == category }.sortedByDescending { it.dateMillis },
            monthTotalCents = total.toLong(),
            budget = budgets.firstOrNull { it.category == category },
            onOpenTransaction = { opened = null; onOpenTransaction(it) },
            onDismiss = { opened = null },
        )
    }
}

/** One category's spending this month: total, share of all spending, budget, and every entry. */
@Composable
private fun CategoryDetailsDialog(
    category: String,
    entries: List<Transaction>,
    monthTotalCents: Long,
    budget: Budget?,
    onOpenTransaction: (Transaction) -> Unit,
    onDismiss: () -> Unit,
) {
    val spent = entries.sumOf { it.amountCents }
    val share = if (monthTotalCents > 0) (spent * 100.0 / monthTotalCents).roundToInt() else 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(category) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Text(formatMoney(spent), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ExpenseColor)
                    Text(
                        stringResource(R.string.home_category_summary, entries.size, share, formatMoney(entries.maxOf { it.amountCents })),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (budget != null) {
                        val left = budget.monthlyLimitCents - spent
                        Text(
                            stringResource(R.string.budget_spent, formatMoney(spent), formatMoney(budget.monthlyLimitCents)) + " · " +
                                if (left < 0) stringResource(R.string.budget_over, formatMoney(-left))
                                else stringResource(R.string.budget_left, formatMoney(left)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (left < 0) MaterialTheme.colorScheme.error else Color.Unspecified,
                        )
                    }
                }
                items(entries, key = { it.id }) { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenTransaction(tx) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dateLabel(tx.dateMillis), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            val details = listOf(tx.party, tx.note, tx.paymentMethod).filter { it.isNotBlank() }
                            if (details.isNotEmpty()) {
                                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("-${formatMoney(tx.amountCents)}", fontWeight = FontWeight.SemiBold, color = ExpenseColor)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

private const val NO_ACCOUNT = "No account"

/** Income and expense for each month of the year being viewed. Months with no entries are left out. */
@Composable
private fun YearSummary(all: List<Transaction>, monthStart: Long) {
    val year = Calendar.getInstance().apply { timeInMillis = monthStart }.get(Calendar.YEAR)
    val months = (0..11).map { month ->
        val start = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = addMonths(start, 1)
        val inMonth = all.filter { it.dateMillis >= start && it.dateMillis < end }
        val income = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
        val expense = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
        Triple(start, income, expense)
    }.filter { (_, income, expense) -> income > 0 || expense > 0 }

    // Show the latest few months, so the list stays short as data grows.
    var showAll by remember { mutableStateOf(false) }
    val visibleMonths = if (showAll) months else months.takeLast(3)

    SectionCard {
        Text(
            stringResource(R.string.home_year, year),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (months.isEmpty()) {
            Text(stringResource(R.string.home_no_entries_year), style = MaterialTheme.typography.bodySmall)
        }
        visibleMonths.forEach { (start, income, expense) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(monthLabel(start), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text("+${formatMoney(income)}", style = MaterialTheme.typography.bodySmall, color = IncomeColor)
                Spacer(Modifier.width(8.dp))
                Text("-${formatMoney(expense)}", style = MaterialTheme.typography.bodySmall, color = ExpenseColor)
            }
        }
        if (months.size > 3) {
            TextButton(onClick = { showAll = !showAll }) {
                Text(
                    if (showAll) stringResource(R.string.home_show_fewer)
                    else stringResource(R.string.home_show_all_months, months.size),
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, onClick: () -> Unit, onDelete: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val sign = if (isIncome) "+" else "-"
    val fromLabel = if (transaction.party.isNotBlank()) {
        if (isIncome) stringResource(R.string.home_from, transaction.party) else stringResource(R.string.home_to, transaction.party)
    } else {
        ""
    }
    val riba = stringResource(R.string.home_interest_tag)
    val halal = if (isIncome && transaction.halalStatus != HalalStatus.NONE) halalLabel(transaction.halalStatus) else ""

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 0.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val details = buildList {
                    add(dateLabel(transaction.dateMillis))
                    if (fromLabel.isNotBlank()) add(fromLabel)
                    if (transaction.note.isNotBlank()) add(transaction.note)
                    if (transaction.paymentMethod.isNotBlank()) add(transaction.paymentMethod)
                    if (halal.isNotBlank()) add(halal)
                    if (transaction.isRiba) add(riba)
                }
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "$sign${formatMoney(transaction.amountCents)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeColor else ExpenseColor,
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun halalLabel(status: HalalStatus): String = when (status) {
    HalalStatus.NONE -> stringResource(R.string.home_halal_not_set)
    HalalStatus.HALAL -> stringResource(R.string.halal_halal)
    HalalStatus.DOUBTFUL -> stringResource(R.string.halal_doubtful)
    HalalStatus.HARAM -> stringResource(R.string.halal_haram)
}

@Composable
private fun TransactionDialog(
    existing: Transaction?,
    shared: SharedReceipt? = null,
    categories: List<Category>,
    allTransactions: List<Transaction>,
    onDismiss: () -> Unit,
    accounts: List<Account>,
    onSave: (TransactionType, String, String, String, String, String, HalalStatus, Boolean, Boolean, Long?, String?) -> Boolean,
) {
    var accountName by remember {
        // New entries go to Cash by default. Existing entries keep their own account.
        mutableStateOf(
            accounts.firstOrNull { it.id == existing?.accountId }?.name
                ?: (if (existing == null) accounts.firstOrNull { it.name == "Cash" }?.name else null) ?: ""
        )
    }
    val context = LocalContext.current
    var receiptName by remember { mutableStateOf(existing?.receiptPath ?: shared?.receiptName) }
    val pickReceipt = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) saveReceipt(context, uri)?.let { receiptName = it }
    }
    val preview = remember(receiptName) { receiptName?.let { loadReceipt(context, it) } }
    val sharedIncome = shared?.text?.isIncome == true
    var type by remember {
        mutableStateOf(existing?.type ?: if (sharedIncome) TransactionType.INCOME else TransactionType.EXPENSE)
    }
    var amount by remember {
        mutableStateOf(existing?.let { formatPlain(it.amountCents) } ?: shared?.text?.amountCents?.let(::formatPlain) ?: "")
    }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var party by remember { mutableStateOf(existing?.party ?: shared?.text?.party ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: if (shared != null) "UPI" else "Cash") }
    var halal by remember { mutableStateOf(existing?.halalStatus ?: HalalStatus.NONE) }
    var riba by remember { mutableStateOf(existing?.isRiba ?: false) }
    var recurring by remember { mutableStateOf(existing?.recurringMonthly ?: false) }
    val errorText = stringResource(R.string.home_error_transaction)
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    shared != null -> stringResource(R.string.share_dialog_title)
                    existing == null -> stringResource(R.string.home_add_transaction)
                    else -> stringResource(R.string.home_edit_transaction)
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (shared != null) {
                    Text(stringResource(R.string.share_dialog_hint), style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { if (type != TransactionType.EXPENSE) category = ""; type = TransactionType.EXPENSE },
                        label = { Text(stringResource(R.string.type_expense)) },
                    )
                    AppChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { if (type != TransactionType.INCOME) category = ""; type = TransactionType.INCOME },
                        label = { Text(stringResource(R.string.type_income)) },
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.label_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                CategorySelector(
                    label = stringResource(R.string.home_label_category),
                    options = categoryOptions(type, categories, allTransactions),
                    selected = category,
                    onSelect = { category = it },
                    type = type,
                )
                OutlinedTextField(
                    value = party,
                    onValueChange = { party = it },
                    label = {
                        Text(
                            if (type == TransactionType.INCOME) stringResource(R.string.home_label_from_whom)
                            else stringResource(R.string.home_label_to_whom),
                        )
                    },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.home_label_for_what)) },
                    singleLine = true,
                )
                CategorySelector(
                    label = stringResource(R.string.home_label_payment_method),
                    options = paymentMethodOptions,
                    selected = paymentMethod,
                    onSelect = { paymentMethod = it },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pickReceipt.launch("image/*") }) {
                        Text(
                            if (receiptName == null) stringResource(R.string.home_attach_receipt)
                            else stringResource(R.string.home_replace_receipt),
                        )
                    }
                    if (receiptName != null) {
                        TextButton(onClick = { receiptName = null }) { Text(stringResource(R.string.home_remove_receipt)) }
                    }
                }
                preview?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(R.string.home_receipt),
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                    )
                }
                if (accounts.isNotEmpty()) {
                    CategorySelector(
                        label = stringResource(R.string.home_label_account),
                        options = listOf(NO_ACCOUNT) + accounts.map { it.name },
                        selected = accountName.ifBlank { NO_ACCOUNT },
                        onSelect = { accountName = if (it == NO_ACCOUNT) "" else it },
                    )
                }
                if (type == TransactionType.INCOME) {
                    Text(stringResource(R.string.home_label_islamic_status), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(HalalStatus.NONE, HalalStatus.HALAL, HalalStatus.DOUBTFUL, HalalStatus.HARAM).forEach { status ->
                            AppChip(
                                selected = halal == status,
                                onClick = { halal = status },
                                label = { Text(halalLabel(status)) },
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = riba, onCheckedChange = { riba = it })
                        Text(stringResource(R.string.home_riba_label))
                    }
                }
                // Only the original entry can repeat. Copies made from it are already linked.
                if (existing == null || existing.sourceId == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recurring, onCheckedChange = { recurring = it })
                        Text(stringResource(R.string.home_repeat_monthly))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val accountId = accounts.firstOrNull { it.name == accountName }?.id
                if (!onSave(type, amount, category, party, note, paymentMethod, halal, riba, recurring, accountId, receiptName)) {
                    error = errorText
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
