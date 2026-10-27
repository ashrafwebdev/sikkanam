package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.Account
import com.ashrafwebdev.sikkanam.data.Transfer

/** Account balances, recent transfers, and actions to add, edit, or delete them. */
@Composable
fun AccountsCard(vm: FinanceViewModel) {
    val accounts by vm.accounts.collectAsState()
    val balances by vm.accountBalances.collectAsState()
    val transfers by vm.transfers.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var editingTransfer by remember { mutableStateOf<Transfer?>(null) }
    val unknown = stringResource(R.string.account_unknown)

    SectionCard {
        SectionHeader(title = stringResource(R.string.account_title))
        if (accounts.isEmpty()) {
            Text(stringResource(R.string.account_empty), style = MaterialTheme.typography.bodySmall)
        }
        accounts.forEach { account ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(account.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    formatMoney(balances[account.id] ?: account.openingCents),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { editingAccount = account }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.account_edit_desc))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { showAdd = true }) { Text(stringResource(R.string.account_add)) }
            if (accounts.size >= 2) {
                FilledTonalButton(onClick = { showTransfer = true }) { Text(stringResource(R.string.account_transfer)) }
            }
        }

        if (transfers.isNotEmpty()) {
            Text(stringResource(R.string.account_recent), style = MaterialTheme.typography.labelLarge)
            transfers.take(5).forEach { t ->
                val from = accounts.firstOrNull { it.id == t.fromAccountId }?.name ?: unknown
                val to = accounts.firstOrNull { it.id == t.toAccountId }?.name ?: unknown
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.account_from_to, from, to), style = MaterialTheme.typography.bodyMedium)
                        Text("${formatMoney(t.amountCents)} · ${dateLabel(t.dateMillis)}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { editingTransfer = t }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.account_edit_transfer_desc))
                    }
                    IconButton(onClick = { vm.deleteTransfer(t.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.account_delete_transfer_desc))
                    }
                }
            }
        }
    }

    if (showAdd) {
        AccountDialog(
            existing = null,
            onDismiss = { showAdd = false },
            onSave = { name, opening ->
                if (vm.addAccount(name, opening)) {
                    showAdd = false
                    true
                } else {
                    false
                }
            },
        )
    }

    editingAccount?.let { account ->
        AccountDialog(
            existing = account,
            onDismiss = { editingAccount = null },
            onSave = { name, opening ->
                if (vm.updateAccount(account, name, opening)) {
                    editingAccount = null
                    true
                } else {
                    false
                }
            },
        )
    }

    if (showTransfer) {
        TransferDialog(
            accounts = accounts,
            existing = null,
            onDismiss = { showTransfer = false },
            onSave = { from, to, amount, note ->
                if (vm.addTransfer(from, to, amount, note)) {
                    showTransfer = false
                    true
                } else {
                    false
                }
            },
        )
    }

    editingTransfer?.let { transfer ->
        TransferDialog(
            accounts = accounts,
            existing = transfer,
            onDismiss = { editingTransfer = null },
            onSave = { from, to, amount, note ->
                if (vm.updateTransfer(transfer, from, to, amount, note)) {
                    editingTransfer = null
                    true
                } else {
                    false
                }
            },
        )
    }
}

@Composable
private fun AccountDialog(existing: Account?, onDismiss: () -> Unit, onSave: (String, String) -> Boolean) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var opening by remember {
        mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.openingCents / 100.0) } ?: "0")
    }
    var error by remember { mutableStateOf<String?>(null) }
    val accountError = stringResource(R.string.account_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) stringResource(R.string.account_new_title) else stringResource(R.string.account_edit_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.account_name_hint)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = opening,
                    onValueChange = { opening = it },
                    label = { Text(stringResource(R.string.account_opening)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(name, opening)) error = accountError }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun TransferDialog(
    accounts: List<Account>,
    existing: Transfer?,
    onDismiss: () -> Unit,
    onSave: (Long, Long, String, String) -> Boolean,
) {
    var from by remember { mutableStateOf(accounts.firstOrNull { it.id == existing?.fromAccountId }?.name ?: accounts.firstOrNull()?.name ?: "") }
    var to by remember { mutableStateOf(accounts.firstOrNull { it.id == existing?.toAccountId }?.name ?: accounts.getOrNull(1)?.name ?: "") }
    var amount by remember {
        mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.amountCents / 100.0) } ?: "")
    }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val transferError = stringResource(R.string.account_transfer_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) stringResource(R.string.account_transfer_title) else stringResource(R.string.account_edit_transfer_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategorySelector(
                    label = stringResource(R.string.account_from),
                    options = accounts.map { it.name },
                    selected = from,
                    onSelect = { from = it },
                )
                CategorySelector(
                    label = stringResource(R.string.account_to_label),
                    options = accounts.map { it.name },
                    selected = to,
                    onSelect = { to = it },
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.label_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.goal_note_optional)) },
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val fromId = accounts.firstOrNull { it.name == from }?.id
                val toId = accounts.firstOrNull { it.name == to }?.id
                val saved = fromId != null && toId != null && onSave(fromId, toId, amount, note)
                if (!saved) error = transferError
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
