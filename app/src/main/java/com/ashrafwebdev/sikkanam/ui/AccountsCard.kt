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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    SectionCard {
        SectionHeader(title = "Accounts")
        if (accounts.isEmpty()) {
            Text("Add cash, bank, or card accounts to see where your money is.", style = MaterialTheme.typography.bodySmall)
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
                    Icon(Icons.Default.Edit, contentDescription = "Edit account")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { showAdd = true }) { Text("Add account") }
            if (accounts.size >= 2) {
                FilledTonalButton(onClick = { showTransfer = true }) { Text("Transfer") }
            }
        }

        if (transfers.isNotEmpty()) {
            Text("Recent transfers", style = MaterialTheme.typography.labelLarge)
            transfers.take(5).forEach { t ->
                val from = accounts.firstOrNull { it.id == t.fromAccountId }?.name ?: "Unknown"
                val to = accounts.firstOrNull { it.id == t.toAccountId }?.name ?: "Unknown"
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("$from to $to", style = MaterialTheme.typography.bodyMedium)
                        Text("${formatMoney(t.amountCents)} · ${dateLabel(t.dateMillis)}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { editingTransfer = t }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit transfer")
                    }
                    IconButton(onClick = { vm.deleteTransfer(t.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete transfer")
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New account" else "Edit account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name, for example Cash or SBI") }, singleLine = true)
                OutlinedTextField(
                    value = opening,
                    onValueChange = { opening = it },
                    label = { Text("Opening balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!onSave(name, opening)) error = "Enter a name and a balance of zero or more." }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Transfer money" else "Edit transfer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategorySelector(label = "From", options = accounts.map { it.name }, selected = from, onSelect = { from = it })
                CategorySelector(label = "To", options = accounts.map { it.name }, selected = to, onSelect = { to = it })
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val fromId = accounts.firstOrNull { it.name == from }?.id
                val toId = accounts.firstOrNull { it.name == to }?.id
                val saved = fromId != null && toId != null && onSave(fromId, toId, amount, note)
                if (!saved) error = "Choose two different accounts and an amount above zero."
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
