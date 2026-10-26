package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.Account

/** Account balances, with actions to add an account or move money between two. */
@Composable
fun AccountsCard(vm: FinanceViewModel) {
    val accounts by vm.accounts.collectAsState()
    val balances by vm.accountBalances.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }

    SectionCard {
        SectionHeader(title = "Accounts")
        if (accounts.isEmpty()) {
            Text("Add cash, bank, or card accounts to see where your money is.", style = MaterialTheme.typography.bodySmall)
        }
        accounts.forEach { account ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(account.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    formatMoney(balances[account.id] ?: account.openingCents),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { showAdd = true }) { Text("Add account") }
            if (accounts.size >= 2) {
                FilledTonalButton(onClick = { showTransfer = true }) { Text("Transfer") }
            }
        }
    }

    if (showAdd) {
        AddAccountDialog(
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

    if (showTransfer) {
        TransferDialog(
            accounts = accounts,
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
}

@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onSave: (String, String) -> Boolean) {
    var name by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name, for example Cash or SBI") }, singleLine = true)
                OutlinedTextField(
                    value = opening,
                    onValueChange = { opening = it },
                    label = { Text("Current balance") },
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
    onDismiss: () -> Unit,
    onSave: (Long, Long, String, String) -> Boolean,
) {
    var from by remember { mutableStateOf(accounts.firstOrNull()?.name ?: "") }
    var to by remember { mutableStateOf(accounts.getOrNull(1)?.name ?: "") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer money") },
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
            }) { Text("Transfer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
