package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.HalalStatus
import com.ashrafwebdev.sikkanam.data.TransactionType
import com.ashrafwebdev.sikkanam.payments.PendingPayments
import java.math.BigDecimal

/** Payments found in payment-app notifications, each waiting for the user to press Add or Dismiss. */
@Composable
fun PendingPaymentsCard(vm: FinanceViewModel) {
    val context = LocalContext.current
    remember { PendingPayments.load(context) }
    val items by PendingPayments.items.collectAsState()
    if (items.isEmpty()) return

    SectionCard {
        SectionHeader(title = stringResource(R.string.payments_title, items.size))
        Text(
            stringResource(R.string.payments_info),
            style = MaterialTheme.typography.bodySmall,
        )
        items.take(5).forEach { payment ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(R.string.payments_to, formatMoney(payment.amountCents), payment.payee),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.payments_app_date, payment.app, dateLabel(payment.detectedMillis)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = {
                    val added = vm.saveTransaction(
                        existing = null,
                        type = TransactionType.EXPENSE,
                        amountText = BigDecimal.valueOf(payment.amountCents, 2).toPlainString(),
                        category = "Other",
                        note = "${payment.app} payment",
                        halal = HalalStatus.NONE,
                        isRiba = false,
                        recurringMonthly = false,
                        party = payment.payee,
                        paymentMethod = "UPI",
                    )
                    if (added) PendingPayments.remove(context, payment.key)
                }) { Text(stringResource(R.string.payments_add)) }
                TextButton(onClick = { PendingPayments.remove(context, payment.key) }) {
                    Text(stringResource(R.string.payments_dismiss))
                }
            }
        }
        if (items.size > 5) {
            Text(stringResource(R.string.payments_more, items.size), style = MaterialTheme.typography.bodySmall)
        }
    }
}
