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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
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
    var editingDonation by remember { mutableStateOf<Donation?>(null) }

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
                    stringResource(R.string.charity_purification_due),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    formatMoney(purificationDue),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(R.string.charity_haram_summary, formatMoney(haramIncome), formatMoney(purificationPaid)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (doubtfulIncome > 0) {
                    Text(
                        stringResource(R.string.charity_doubtful, formatMoney(doubtfulIncome)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = stringResource(R.string.charity_sadaqah_given),
                    value = formatMoney(sadaqahGiven),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StatTile(
                    label = stringResource(R.string.charity_purification_paid),
                    value = formatMoney(purificationPaid),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.charity_payments),
                action = {
                    FilledTonalButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.charity_record))
                    }
                },
            )
        }

        if (donations.isEmpty()) {
            item { EmptyState(stringResource(R.string.charity_empty)) }
        }

        items(donations, key = { it.id }) { donation ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (donation.kind == DonationKind.SADAQAH) stringResource(R.string.charity_sadaqah)
                            else stringResource(R.string.charity_purification),
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
                    IconButton(onClick = { editingDonation = donation }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.charity_edit_desc))
                    }
                    IconButton(onClick = { vm.deleteDonation(donation.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.charity_delete_desc))
                    }
                }
            }
        }
    }

    if (showDialog) {
        DonationDialog(
            existing = null,
            onDismiss = { showDialog = false },
            onSave = { kind, amount, note ->
                if (vm.saveDonation(kind, amount, note)) {
                    showDialog = false
                    true
                } else {
                    false
                }
            },
        )
    }

    editingDonation?.let { donation ->
        DonationDialog(
            existing = donation,
            onDismiss = { editingDonation = null },
            onSave = { kind, amount, note ->
                if (vm.updateDonation(donation, kind, amount, note)) {
                    editingDonation = null
                    true
                } else {
                    false
                }
            },
        )
    }
}

private fun List<Transaction>.incomeWith(status: HalalStatus): Long =
    filter { it.type == TransactionType.INCOME && it.halalStatus == status }.sumOf { it.amountCents }

@Composable
private fun DonationDialog(
    existing: Donation?,
    onDismiss: () -> Unit,
    onSave: (DonationKind, String, String) -> Boolean,
) {
    var kind by remember { mutableStateOf(existing?.kind ?: DonationKind.SADAQAH) }
    var amount by remember {
        mutableStateOf(existing?.let { String.format(java.util.Locale.US, "%.2f", it.amountCents / 100.0) } ?: "")
    }
    var note by remember { mutableStateOf(existing?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) stringResource(R.string.charity_record_title) else stringResource(R.string.charity_edit_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == DonationKind.SADAQAH,
                        onClick = { kind = DonationKind.SADAQAH },
                        label = { Text(stringResource(R.string.charity_sadaqah)) },
                    )
                    FilterChip(
                        selected = kind == DonationKind.PURIFICATION,
                        onClick = { kind = DonationKind.PURIFICATION },
                        label = { Text(stringResource(R.string.charity_purification)) },
                    )
                }
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
            }
        },
        // Invalid amounts keep the dialog open, so the user can correct them.
        confirmButton = {
            TextButton(onClick = { onSave(kind, amount, note) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
