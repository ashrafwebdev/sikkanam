package com.ashrafwebdev.sikkanam.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.contacts.hasContactsPermission
import com.ashrafwebdev.sikkanam.contacts.saveToPhoneContacts
import com.ashrafwebdev.sikkanam.data.GroupKind
import com.ashrafwebdev.sikkanam.data.TripContribution
import com.ashrafwebdev.sikkanam.data.TripExpense
import com.ashrafwebdev.sikkanam.data.TripGroup
import com.ashrafwebdev.sikkanam.data.TripMember
import com.ashrafwebdev.sikkanam.trip.TripSettlement

/** Group trips: members pay in, costs are split equally, and the app shows who settles with whom. */
@Composable
fun TripsScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    var openGroupId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDirectory by rememberSaveable { mutableStateOf(false) }
    val groups by vm.tripGroups.collectAsState()

    val openGroup = groups.firstOrNull { it.id == openGroupId }
    when {
        showDirectory -> DirectoryPage(vm, onBack = { showDirectory = false }, modifier = modifier)
        openGroup != null -> TripDetail(vm, openGroup, onBack = { openGroupId = null }, modifier = modifier)
        else -> TripList(
            vm,
            groups,
            onOpen = { openGroupId = it.id },
            onOpenDirectory = { showDirectory = true },
            modifier = modifier,
        )
    }
}

@Composable
private fun TripList(
    vm: FinanceViewModel,
    groups: List<TripGroup>,
    onOpen: (TripGroup) -> Unit,
    onOpenDirectory: () -> Unit,
    modifier: Modifier,
) {
    val members by vm.tripMembers.collectAsState()
    val expenses by vm.tripExpenses.collectAsState()
    var newKind by remember { mutableStateOf<GroupKind?>(null) }
    val familyLabel = stringResource(R.string.trip_kind_family)
    val tripLabel = stringResource(R.string.trip_kind_trip)

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionHeader(
                title = stringResource(R.string.trip_list_title),
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { newKind = GroupKind.TRIP }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.trip_btn_trip))
                        }
                        FilledTonalButton(onClick = { newKind = GroupKind.FAMILY }) {
                            Text(stringResource(R.string.trip_btn_family))
                        }
                    }
                },
            )
        }
        item { DawatSection(vm) }
        item { LocalitySection(vm) }
        if (groups.isEmpty()) {
            item { EmptyState(stringResource(R.string.trip_empty)) }
        }
        items(groups, key = { it.id }) { group ->
            val spent = expenses.filter { it.groupId == group.id }.sumOf { it.amountCents }
            val count = members.count { it.groupId == group.id }
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(group.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        val label = if (group.kind == GroupKind.FAMILY) familyLabel else tripLabel
                        Text(
                            stringResource(R.string.trip_summary, label, count, formatMoney(spent)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedButton(onClick = { onOpen(group) }) { Text(stringResource(R.string.trip_open)) }
                }
            }
        }
    }

    newKind?.let { kind ->
        NameDialog(
            title = if (kind == GroupKind.FAMILY) stringResource(R.string.trip_new_family) else stringResource(R.string.trip_new_trip),
            label = if (kind == GroupKind.FAMILY) stringResource(R.string.trip_family_name_hint) else stringResource(R.string.trip_name_hint),
            onDismiss = { newKind = null },
            onSave = { name ->
                if (vm.addTripGroup(name, kind)) {
                    newKind = null
                    true
                } else {
                    false
                }
            },
        )
    }
}

@Composable
private fun TripDetail(vm: FinanceViewModel, group: TripGroup, onBack: () -> Unit, modifier: Modifier) {
    val allMembers by vm.tripMembers.collectAsState()
    val allContributions by vm.tripContributions.collectAsState()
    val allExpenses by vm.tripExpenses.collectAsState()

    // A family group is ongoing, so its settle-up is shown one month at a time.
    val isFamily = group.kind == GroupKind.FAMILY
    var monthStart by rememberSaveable { mutableStateOf(startOfMonth(System.currentTimeMillis())) }
    val monthEnd = addMonths(monthStart, 1)
    fun inView(dateMillis: Long) = !isFamily || (dateMillis >= monthStart && dateMillis < monthEnd)

    val members = allMembers.filter { it.groupId == group.id }
    // Only members who are going share costs. Others are listed, but they don't pay or owe.
    val going = members.filter { it.confirmed }
    val contributions = allContributions.filter { it.groupId == group.id && inView(it.dateMillis) }
    val expenses = allExpenses.filter { it.groupId == group.id && inView(it.dateMillis) }

    // Unpaid balances roll forward from the end of the last settled month, until the next settle-up.
    val settledMonths by vm.tripSettledMonths.collectAsState()
    val groupSettled = settledMonths.filter { it.groupId == group.id }
    val isSettled = groupSettled.any { it.monthMillis == monthStart }
    val carryFrom = groupSettled.filter { it.monthMillis < monthStart }.maxOfOrNull { addMonths(it.monthMillis, 1) }
    val carriedIn: Map<Long, Long> = if (!isFamily) {
        emptyMap()
    } else {
        val from = carryFrom ?: Long.MIN_VALUE
        val earlierContributions = allContributions.filter {
            it.groupId == group.id && it.dateMillis >= from && it.dateMillis < monthStart
        }
        val earlierExpenses = allExpenses.filter {
            it.groupId == group.id && it.dateMillis >= from && it.dateMillis < monthStart
        }
        TripSettlement.balances(going, earlierContributions, earlierExpenses, byIncome = group.splitByIncome)
            .associate { it.memberId to it.net }
    }
    val balances = TripSettlement.balances(
        going, contributions, expenses, byIncome = group.splitByIncome, carriedIn = carriedIn,
    )
    val payments = TripSettlement.settlements(balances)

    val totalContributed = contributions.sumOf { it.amountCents }
    val totalSpent = expenses.sumOf { it.amountCents }
    val poolLeft = totalContributed - expenses.filter { it.paidByMemberId == null }.sumOf { it.amountCents }

    var showAddMember by remember { mutableStateOf(false) }
    var showContribution by remember { mutableStateOf(false) }
    var showExpense by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var incomeEditing by remember { mutableStateOf<TripMember?>(null) }
    var renamingMember by remember { mutableStateOf<TripMember?>(null) }
    var renamingGroup by remember { mutableStateOf(false) }
    var editingContribution by remember { mutableStateOf<TripContribution?>(null) }
    var editingExpense by remember { mutableStateOf<TripExpense?>(null) }

    val interestedText = stringResource(R.string.trip_interested)
    val everyoneText = stringResource(R.string.trip_everyone)
    val unknownText = stringResource(R.string.trip_unknown)
    val poolLabel = stringResource(R.string.trip_pool_label)

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.trip_back)) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { renamingGroup = true }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.trip_rename_desc))
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.trip_delete_desc))
                }
            }
            Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (isFamily) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isSettled) stringResource(R.string.trip_settled_this_month) else stringResource(R.string.trip_not_settled_yet),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (carriedIn.values.any { it != 0L }) {
                            Text(stringResource(R.string.trip_carried_note), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (isSettled) {
                        TextButton(onClick = { vm.reopenMonth(group.id, monthStart) }) { Text(stringResource(R.string.trip_reopen)) }
                    } else {
                        FilledTonalButton(onClick = { vm.markMonthSettled(group.id, monthStart) }) {
                            Text(stringResource(R.string.trip_mark_settled))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.trip_split_by_income),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = group.splitByIncome,
                        onCheckedChange = { vm.setSplitByIncome(group, it) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthStart = addMonths(monthStart, -1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = stringResource(R.string.home_previous_month))
                    }
                    Text(
                        stringResource(R.string.trip_settle_for_month, monthLabel(monthStart)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { monthStart = addMonths(monthStart, 1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = stringResource(R.string.home_next_month))
                    }
                }
            }
        }

        item {
            SectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    stringResource(R.string.trip_pool_left, formatMoney(poolLeft)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(
                        R.string.trip_counts,
                        going.size,
                        members.size - going.size,
                        formatMoney(totalContributed),
                        formatMoney(totalSpent),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { showContribution = true }, enabled = members.isNotEmpty()) {
                    Text(stringResource(R.string.trip_add_money_in))
                }
                FilledTonalButton(onClick = { showExpense = true }, enabled = members.isNotEmpty()) {
                    Text(stringResource(R.string.trip_add_expense))
                }
            }
        }

        item { MuqamiAmaalSection(vm, group.id, members) }
        item { WeeklyProgrammeSection(vm, group.id, members) }
        item { KhuroojSection(vm, group.id) }
        item { TripTravelSection(vm, group.id) }
        item { MeetingsSection(vm, group.id) }
        item { DutiesSection(vm, group.id, members.map { it.name }) }
        item { SectionHeader(title = stringResource(R.string.trip_members)) }
        item {
            FilledTonalButton(onClick = { showAddMember = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.trip_add_member))
            }
        }
        items(members, key = { "m${it.id}" }) { member ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(member.name, style = MaterialTheme.typography.bodyMedium)
                    val details = buildList {
                        if (!member.confirmed) add(interestedText)
                        if (member.phone.isNotBlank()) add(member.phone)
                        if (member.days > 0) add(stringResource(R.string.trip_days, member.days))
                        if (member.advanceCents > 0) {
                            add(stringResource(R.string.trip_advance, formatMoney(member.advanceCents)))
                        }
                        if (member.address.isNotBlank()) add(member.address)
                    }
                    if (details.isNotEmpty()) {
                        Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = { renamingMember = member }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.trip_edit_member_desc))
                }
                if (isFamily) {
                    TextButton(onClick = { incomeEditing = member }) {
                        Text(stringResource(R.string.trip_income_button, formatMoney(member.incomeCents)))
                    }
                }
                IconButton(onClick = { vm.deleteTripMember(member) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.trip_remove_member_desc))
                }
            }
        }

        if (balances.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.trip_balances)) }
            items(balances, key = { "b${it.memberId}" }) { b ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            b.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (b.net >= 0) stringResource(R.string.trip_gets, formatMoney(b.net))
                            else stringResource(R.string.trip_owes, formatMoney(-b.net)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (b.net >= 0) IncomeColor else ExpenseColor,
                        )
                    }
                    Text(
                        stringResource(
                            R.string.trip_paid_share,
                            formatMoney(b.contributed),
                            formatMoney(b.paidDirectly),
                            formatMoney(b.share),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (payments.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.trip_settle_up)) }
            items(payments, key = { "p${it.from}-${it.to}-${it.amountCents}" }) { p ->
                SectionCard {
                    Text(
                        if (p.to == "Pool") stringResource(R.string.trip_pays_pool, p.from, formatMoney(p.amountCents))
                        else if (p.from == "Pool") stringResource(R.string.trip_refund_from_pool, formatMoney(p.amountCents), p.to)
                        else stringResource(R.string.trip_pays, p.from, p.to, formatMoney(p.amountCents)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (expenses.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.trip_expenses)) }
            items(expenses, key = { "e${it.id}" }) { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(e.description, style = MaterialTheme.typography.bodyMedium)
                        val payer = members.firstOrNull { it.id == e.paidByMemberId }?.name ?: poolLabel
                        val sharedBy = if (e.participants().isEmpty()) everyoneText
                        else stringResource(R.string.trip_people, e.participants().size)
                        Text(
                            stringResource(R.string.trip_expense_line, dateLabel(e.dateMillis), payer, sharedBy),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(formatMoney(e.amountCents), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { editingExpense = e }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.trip_edit_expense_desc))
                    }
                    IconButton(onClick = { vm.deleteTripExpense(e.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.trip_delete_expense_desc))
                    }
                }
            }
        }

        if (contributions.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.trip_money_in)) }
            items(contributions, key = { "c${it.id}" }) { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            members.firstOrNull { it.id == c.memberId }?.name ?: unknownText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(dateLabel(c.dateMillis), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        formatMoney(c.amountCents),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeColor,
                    )
                    IconButton(onClick = { editingContribution = c }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.trip_edit_payment_desc))
                    }
                    IconButton(onClick = { vm.deleteTripContribution(c.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.trip_delete_payment_desc))
                    }
                }
            }
        }
    }

    if (showContribution) {
        ContributionDialog(
            members = members,
            onDismiss = { showContribution = false },
            onSave = { memberId, amount ->
                if (vm.addTripContribution(group.id, memberId, amount)) {
                    showContribution = false
                    true
                } else {
                    false
                }
            },
        )
    }

    if (showExpense) {
        ExpenseDialog(
            members = going,
            onDismiss = { showExpense = false },
            isFamily = isFamily,
            onSave = { paidBy, description, amount, sharers, recurring ->
                if (vm.addTripExpense(group.id, paidBy, description, amount, sharers, recurring)) {
                    showExpense = false
                    true
                } else {
                    false
                }
            },
        )
    }

    incomeEditing?.let { member ->
        IncomeDialog(
            member = member,
            onDismiss = { incomeEditing = null },
            onSave = { amount ->
                if (vm.setMemberIncome(member, amount)) {
                    incomeEditing = null
                    true
                } else {
                    false
                }
            },
        )
    }

    if (showAddMember) {
        MemberDialog(
            existing = null,
            onDismiss = { showAddMember = false },
            onSave = { name, phone, address, days, advance, going ->
                if (vm.addTripMember(group.id, name, phone, address, days, advance, going)) {
                    showAddMember = false
                    true
                } else {
                    false
                }
            },
        )
    }

    renamingMember?.let { member ->
        MemberDialog(
            existing = member,
            onDismiss = { renamingMember = null },
            onSave = { name, phone, address, days, advance, going ->
                if (vm.updateTripMember(member, name, phone, address, days, advance, going)) {
                    renamingMember = null
                    true
                } else {
                    false
                }
            },
        )
    }

    if (renamingGroup) {
        NameDialog(
            title = stringResource(R.string.trip_rename_desc),
            label = stringResource(R.string.trip_name_label),
            initial = group.name,
            onDismiss = { renamingGroup = false },
            onSave = { name ->
                if (vm.renameTripGroup(group, name)) {
                    renamingGroup = false
                    true
                } else {
                    false
                }
            },
        )
    }

    editingContribution?.let { contribution ->
        ContributionDialog(
            members = members,
            existing = contribution,
            onDismiss = { editingContribution = null },
            onSave = { memberId, amount ->
                if (vm.updateTripContribution(contribution, memberId, amount)) {
                    editingContribution = null
                    true
                } else {
                    false
                }
            },
        )
    }

    editingExpense?.let { expense ->
        ExpenseDialog(
            members = going,
            isFamily = isFamily,
            existing = expense,
            onDismiss = { editingExpense = null },
            onSave = { paidBy, description, amount, sharers, _ ->
                if (vm.updateTripExpense(expense, paidBy, description, amount, sharers)) {
                    editingExpense = null
                    true
                } else {
                    false
                }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.trip_delete_title, group.name)) },
            text = { Text(stringResource(R.string.trip_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTripGroup(group.id)
                    confirmDelete = false
                    onBack()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ContributionDialog(
    members: List<TripMember>,
    existing: TripContribution? = null,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Boolean,
) {
    var name by remember { mutableStateOf(members.firstOrNull { it.id == existing?.memberId }?.name ?: members.firstOrNull()?.name ?: "") }
    var amount by remember {
        mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.amountCents / 100.0) } ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }
    val chooseError = stringResource(R.string.trip_choose_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_money_in)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategorySelector(
                    label = stringResource(R.string.trip_who),
                    options = members.map { it.name },
                    selected = name,
                    onSelect = { name = it },
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.label_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val memberId = members.firstOrNull { it.name == name }?.id
                val saved = memberId != null && onSave(memberId, amount)
                if (!saved) error = chooseError
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun IncomeDialog(member: TripMember, onDismiss: () -> Unit, onSave: (String) -> Boolean) {
    var amount by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", member.incomeCents / 100.0))
    }
    var error by remember { mutableStateOf<String?>(null) }
    val incomeError = stringResource(R.string.trip_income_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_income_title, member.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.trip_income_per_month)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Text(
                    stringResource(R.string.trip_income_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(amount)) error = incomeError }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ExpenseDialog(
    members: List<TripMember>,
    isFamily: Boolean,
    existing: TripExpense? = null,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, List<Long>, Boolean) -> Boolean,
) {
    val poolLabel = stringResource(R.string.trip_pool_label)
    var payer by remember { mutableStateOf(members.firstOrNull { it.id == existing?.paidByMemberId }?.name ?: poolLabel) }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var amount by remember {
        mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.amountCents / 100.0) } ?: "")
    }
    var recurring by remember { mutableStateOf(existing?.recurringMonthly ?: false) }
    // Everyone is ticked by default. Untick the people who did not share this cost.
    var sharers by remember {
        mutableStateOf(existing?.participants()?.takeIf { it.isNotEmpty() }?.toSet() ?: members.map { it.id }.toSet())
    }
    var error by remember { mutableStateOf<String?>(null) }
    val expenseError = stringResource(R.string.trip_expense_error)
    val equalText = stringResource(R.string.trip_split_equal)
    val incomeSplitText = stringResource(R.string.trip_split_by_income_option)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_expense_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.trip_for_what)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.label_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                CategorySelector(
                    label = stringResource(R.string.trip_paid_by),
                    options = listOf(poolLabel) + members.map { it.name },
                    selected = payer,
                    onSelect = { payer = it },
                )
                Text(stringResource(R.string.trip_shared_by), style = MaterialTheme.typography.labelMedium)
                members.forEach { member ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = member.id in sharers,
                            onCheckedChange = { checked ->
                                sharers = if (checked) sharers + member.id else sharers - member.id
                            },
                        )
                        Text(member.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    stringResource(
                        R.string.trip_split_line,
                        if (isFamily) incomeSplitText else equalText,
                        sharers.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (isFamily) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recurring, onCheckedChange = { recurring = it })
                        Text(stringResource(R.string.trip_repeats_monthly))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val paidBy = if (payer == poolLabel) null else members.firstOrNull { it.name == payer }?.id
                val chosen = members.map { it.id }.filter { it in sharers }
                if (!onSave(paidBy, description, amount, chosen, recurring && isFamily)) {
                    error = expenseError
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Form for a member's name, contact number, address, days, and optional advance. */
@Composable
private fun MemberDialog(
    existing: TripMember?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Boolean) -> Boolean,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var going by remember { mutableStateOf(existing?.confirmed ?: false) }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var days by remember { mutableStateOf(if (existing == null || existing.days == 0) "" else existing.days.toString()) }
    var advance by remember {
        mutableStateOf(
            if (existing == null || existing.advanceCents == 0L) "" else String.format(java.util.Locale.US, "%.2f", existing.advanceCents / 100.0),
        )
    }
    var saveToContacts by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val memberError = stringResource(R.string.trip_member_error)
    val contactFailed = stringResource(R.string.trip_toast_contact_failed)
    val contactDenied = stringResource(R.string.trip_toast_contact_denied)

    // Saves the member, then the phone contact if the user asked for it. Contacts are only
    // written after the trip member is saved, so a failed save never leaves a stray contact.
    fun submit(writeContact: Boolean) {
        if (!onSave(name, phone, address, days, advance, going)) {
            error = memberError
            return
        }
        if (writeContact && !saveToPhoneContacts(context, name.trim(), phone.trim())) {
            Toast.makeText(context, contactFailed, Toast.LENGTH_LONG).show()
        }
    }

    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(context, contactDenied, Toast.LENGTH_LONG).show()
        }
        submit(writeContact = granted)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) stringResource(R.string.trip_add_member_title)
                else stringResource(R.string.trip_edit_member_title),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.trip_member_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.trip_contact_number)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.trip_address)) },
                    maxLines = 3,
                )
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it },
                    label = { Text(stringResource(R.string.trip_days_optional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = advance,
                    onValueChange = { advance = it },
                    label = { Text(stringResource(R.string.trip_advance_optional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = going, onCheckedChange = { going = it })
                    Text(stringResource(R.string.trip_going_label))
                }
                if (existing == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = saveToContacts,
                            onCheckedChange = { saveToContacts = it },
                            enabled = phone.isNotBlank(),
                        )
                        Text(stringResource(R.string.trip_save_to_contacts))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val wantContact = existing == null && saveToContacts && phone.isNotBlank()
                if (wantContact && !hasContactsPermission(context)) {
                    contactsPermission.launch(Manifest.permission.WRITE_CONTACTS)
                } else {
                    submit(writeContact = wantContact)
                }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Boolean,
) {
    var name by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    val nameError = stringResource(R.string.trip_name_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(label) }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(name)) error = nameError }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
