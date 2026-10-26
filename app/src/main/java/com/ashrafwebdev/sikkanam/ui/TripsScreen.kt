package com.ashrafwebdev.sikkanam.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import com.ashrafwebdev.sikkanam.data.GroupKind
import com.ashrafwebdev.sikkanam.data.TripGroup
import com.ashrafwebdev.sikkanam.data.TripMember
import com.ashrafwebdev.sikkanam.trip.TripSettlement

private const val POOL_LABEL = "Shared pool"

/** Group trips: members pay in, costs are split equally, and the app shows who settles with whom. */
@Composable
fun TripsScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    var openGroupId by rememberSaveable { mutableStateOf<Long?>(null) }
    val groups by vm.tripGroups.collectAsState()

    val openGroup = groups.firstOrNull { it.id == openGroupId }
    if (openGroup != null) {
        TripDetail(vm, openGroup, onBack = { openGroupId = null }, modifier = modifier)
    } else {
        TripList(vm, groups, onOpen = { openGroupId = it.id }, modifier = modifier)
    }
}

@Composable
private fun TripList(vm: FinanceViewModel, groups: List<TripGroup>, onOpen: (TripGroup) -> Unit, modifier: Modifier) {
    val members by vm.tripMembers.collectAsState()
    val expenses by vm.tripExpenses.collectAsState()
    var newKind by remember { mutableStateOf<GroupKind?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionHeader(
                title = "Trips and family",
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { newKind = GroupKind.TRIP }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Trip")
                        }
                        FilledTonalButton(onClick = { newKind = GroupKind.FAMILY }) {
                            Text("Family")
                        }
                    }
                },
            )
        }
        if (groups.isEmpty()) {
            item { EmptyState("No trips yet. Create one for a tour and invite everyone to share the costs.") }
        }
        items(groups, key = { it.id }) { group ->
            val spent = expenses.filter { it.groupId == group.id }.sumOf { it.amountCents }
            val count = members.count { it.groupId == group.id }
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(group.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        val label = if (group.kind == GroupKind.FAMILY) "Family" else "Trip"
                        Text("$label · $count members · spent ${formatMoney(spent)}", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { onOpen(group) }) { Text("Open") }
                }
            }
        }
    }

    newKind?.let { kind ->
        NameDialog(
            title = if (kind == GroupKind.FAMILY) "New family group" else "New trip",
            label = if (kind == GroupKind.FAMILY) "Family name, for example Joint family" else "Trip name, for example Ooty tour",
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
        TripSettlement.balances(members, earlierContributions, earlierExpenses, byIncome = group.splitByIncome)
            .associate { it.memberId to it.net }
    }
    val balances = TripSettlement.balances(
        members, contributions, expenses, byIncome = group.splitByIncome, carriedIn = carriedIn,
    )
    val payments = TripSettlement.settlements(balances)

    val totalContributed = contributions.sumOf { it.amountCents }
    val totalSpent = expenses.sumOf { it.amountCents }
    val poolLeft = totalContributed - expenses.filter { it.paidByMemberId == null }.sumOf { it.amountCents }

    var newMember by remember { mutableStateOf("") }
    var memberError by remember { mutableStateOf<String?>(null) }
    var showContribution by remember { mutableStateOf(false) }
    var showExpense by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var incomeEditing by remember { mutableStateOf<TripMember?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("Back") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete trip")
                }
            }
            Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (isFamily) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isSettled) "Settled for this month" else "Not settled yet",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (carriedIn.values.any { it != 0L }) {
                            Text("Includes balances brought forward", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (isSettled) {
                        TextButton(onClick = { vm.reopenMonth(group.id, monthStart) }) { Text("Reopen") }
                    } else {
                        FilledTonalButton(onClick = { vm.markMonthSettled(group.id, monthStart) }) { Text("Mark settled") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Split by income", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = group.splitByIncome,
                        onCheckedChange = { vm.setSplitByIncome(group, it) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthStart = addMonths(monthStart, -1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        "Settle-up for ${monthLabel(monthStart)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { monthStart = addMonths(monthStart, 1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            }
        }

        item {
            SectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "Pool left ${formatMoney(poolLeft)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Collected ${formatMoney(totalContributed)} · spent ${formatMoney(totalSpent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { showContribution = true }, enabled = members.isNotEmpty()) {
                    Text("Add money in")
                }
                FilledTonalButton(onClick = { showExpense = true }, enabled = members.isNotEmpty()) {
                    Text("Add expense")
                }
            }
        }

        item { SectionHeader(title = "Members") }
        item {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newMember,
                        onValueChange = {
                            newMember = it
                            memberError = null
                        },
                        label = { Text("Name") },
                        singleLine = true,
                        isError = memberError != null,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(onClick = {
                        if (vm.addTripMember(group.id, newMember)) {
                            newMember = ""
                        } else {
                            memberError = "Enter a unique name."
                        }
                    }) { Text("Add") }
                }
                memberError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
        items(members, key = { "m${it.id}" }) { member ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (isFamily) {
                    TextButton(onClick = { incomeEditing = member }) {
                        Text("Income ${formatMoney(member.incomeCents)}")
                    }
                }
                IconButton(onClick = { vm.deleteTripMember(member) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove member")
                }
            }
        }

        if (balances.isNotEmpty()) {
            item { SectionHeader(title = "Balances") }
            items(balances, key = { "b${it.memberId}" }) { b ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            if (b.net >= 0) "gets ${formatMoney(b.net)}" else "owes ${formatMoney(-b.net)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (b.net >= 0) IncomeColor else ExpenseColor,
                        )
                    }
                    Text(
                        "Paid in ${formatMoney(b.contributed)} · paid out ${formatMoney(b.paidDirectly)} · share ${formatMoney(b.share)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (payments.isNotEmpty()) {
            item { SectionHeader(title = "Settle up") }
            items(payments, key = { "p${it.from}-${it.to}-${it.amountCents}" }) { p ->
                SectionCard {
                    Text(
                        if (p.to == "Pool") "${p.from} pays the pool ${formatMoney(p.amountCents)}"
                        else if (p.from == "Pool") "Refund ${formatMoney(p.amountCents)} to ${p.to} from the pool"
                        else "${p.from} pays ${p.to} ${formatMoney(p.amountCents)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (expenses.isNotEmpty()) {
            item { SectionHeader(title = "Expenses") }
            items(expenses, key = { "e${it.id}" }) { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(e.description, style = MaterialTheme.typography.bodyMedium)
                        val payer = members.firstOrNull { it.id == e.paidByMemberId }?.name ?: POOL_LABEL
                        val sharedBy = if (e.participants().isEmpty()) "everyone" else "${e.participants().size} people"
                        Text(
                            "${dateLabel(e.dateMillis)} · paid by $payer · shared by $sharedBy",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(formatMoney(e.amountCents), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { vm.deleteTripExpense(e.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete expense")
                    }
                }
            }
        }

        if (contributions.isNotEmpty()) {
            item { SectionHeader(title = "Money in") }
            items(contributions, key = { "c${it.id}" }) { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(members.firstOrNull { it.id == c.memberId }?.name ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
                        Text(dateLabel(c.dateMillis), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(formatMoney(c.amountCents), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = IncomeColor)
                    IconButton(onClick = { vm.deleteTripContribution(c.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete payment in")
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
            members = members,
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

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${group.name}?") },
            text = { Text("This removes the trip, its members, and all its money records.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTripGroup(group.id)
                    confirmDelete = false
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ContributionDialog(
    members: List<TripMember>,
    onDismiss: () -> Unit,
    onSave: (Long, String) -> Boolean,
) {
    var name by remember { mutableStateOf(members.firstOrNull()?.name ?: "") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Money in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategorySelector(label = "Who", options = members.map { it.name }, selected = name, onSelect = { name = it })
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
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
                if (!saved) error = "Choose a member and enter an amount above zero."
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun IncomeDialog(member: TripMember, onDismiss: () -> Unit, onSave: (String) -> Boolean) {
    var amount by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", member.incomeCents / 100.0))
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly income of ${member.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Income per month") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Text(
                    "Used only when the family splits costs by income.",
                    style = MaterialTheme.typography.bodySmall,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(amount)) error = "Enter an amount, zero or more." }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ExpenseDialog(
    members: List<TripMember>,
    isFamily: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String, List<Long>, Boolean) -> Boolean,
) {
    var payer by remember { mutableStateOf(POOL_LABEL) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var recurring by remember { mutableStateOf(false) }
    // Everyone is ticked by default. Untick the people who did not share this cost.
    var sharers by remember { mutableStateOf(members.map { it.id }.toSet()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trip expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("For what") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                CategorySelector(
                    label = "Paid by",
                    options = listOf(POOL_LABEL) + members.map { it.name },
                    selected = payer,
                    onSelect = { payer = it },
                )
                Text("Shared by", style = MaterialTheme.typography.labelMedium)
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
                    "Split ${if (isFamily) "equally, or by income if the family chose that" else "equally"} among the ${sharers.size} people ticked.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (isFamily) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recurring, onCheckedChange = { recurring = it })
                        Text("Repeats every month")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val paidBy = if (payer == POOL_LABEL) null else members.firstOrNull { it.name == payer }?.id
                val chosen = members.map { it.id }.filter { it in sharers }
                if (!onSave(paidBy, description, amount, chosen, recurring && isFamily)) {
                    error = "Enter what for, an amount above zero, and at least one person sharing it."
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NameDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (String) -> Boolean) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

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
            TextButton(onClick = { if (!onSave(name)) error = "Enter a name." }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
