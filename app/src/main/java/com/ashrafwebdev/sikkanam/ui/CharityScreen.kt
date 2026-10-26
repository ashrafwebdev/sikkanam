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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import com.ashrafwebdev.sikkanam.data.Donation
import com.ashrafwebdev.sikkanam.data.DonationKind
import com.ashrafwebdev.sikkanam.data.HalalStatus
import com.ashrafwebdev.sikkanam.data.Transaction
import com.ashrafwebdev.sikkanam.data.TransactionType

@Composable
fun CharityScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    val transactions by vm.transactions.collectAsState()
    val donations by vm.donations.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Haram income must be purified (given away). Doubtful income should be reviewed.
    val haramIncome = transactions.incomeWith(HalalStatus.HARAM)
    val doubtfulIncome = transactions.incomeWith(HalalStatus.DOUBTFUL)
    val purificationPaid = donations.filter { it.kind == DonationKind.PURIFICATION }.sumOf { it.amountCents }
    val sadaqahGiven = donations.filter { it.kind == DonationKind.SADAQAH }.sumOf { it.amountCents }
    val purificationDue = (haramIncome - purificationPaid).coerceAtLeast(0L)

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "Purification due",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    formatMoney(purificationDue),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Haram income ${formatMoney(haramIncome)} · purified ${formatMoney(purificationPaid)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (doubtfulIncome > 0) {
                    Text(
                        "Doubtful income to review: ${formatMoney(doubtfulIncome)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = "Sadaqah given",
                    value = formatMoney(sadaqahGiven),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatTile(
                    label = "Purification paid",
                    value = formatMoney(purificationPaid),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SectionHeader(
                title = "Payments",
                action = {
                    FilledTonalButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Record")
                    }
                },
            )
        }

        if (donations.isEmpty()) {
            item { EmptyState("No payments yet. Record sadaqah or purification here.") }
        }

        items(donations, key = { it.id }) { donation ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (donation.kind == DonationKind.SADAQAH) "Sadaqah" else "Purification",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val details = listOf(dateLabel(donation.dateMillis), donation.note).filter { it.isNotBlank() }
                        Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        formatMoney(donation.amountCents),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { vm.deleteDonation(donation.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete payment")
                    }
                }
            }
        }
    }

    if (showDialog) {
        DonationDialog(
            onDismiss = { showDialog = false },
            onSave = { kind, amount, note ->
                if (vm.saveDonation(kind, amount, note)) showDialog = false
            },
        )
    }
}

private fun List<Transaction>.incomeWith(status: HalalStatus): Long =
    filter { it.type == TransactionType.INCOME && it.halalStatus == status }.sumOf { it.amountCents }

@Composable
private fun DonationDialog(
    onDismiss: () -> Unit,
    onSave: (DonationKind, String, String) -> Unit,
) {
    var kind by remember { mutableStateOf(DonationKind.SADAQAH) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == DonationKind.SADAQAH,
                        onClick = { kind = DonationKind.SADAQAH },
                        label = { Text("Sadaqah") },
                    )
                    FilterChip(
                        selected = kind == DonationKind.PURIFICATION,
                        onClick = { kind = DonationKind.PURIFICATION },
                        label = { Text("Purification") },
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(kind, amount, note) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
