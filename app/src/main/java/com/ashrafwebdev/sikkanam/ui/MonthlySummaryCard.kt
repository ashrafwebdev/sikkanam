package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.TransactionType
import java.util.Calendar

/**
 * In the first week of a month, shows a short review of the previous month: money in and out,
 * money put into savings goals, and any budgets that were exceeded. Hidden for the rest of the month.
 */
@Composable
fun MonthlySummaryCard(vm: FinanceViewModel) {
    val transactions by vm.transactions.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val goalEntries by vm.goalEntries.collectAsState()

    val now = System.currentTimeMillis()
    val thisMonth = startOfMonth(now)
    if (Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_MONTH) > 7) return

    val prevStart = addMonths(thisMonth, -1)
    val prevEnd = thisMonth
    val prevTx = transactions.filter { it.dateMillis >= prevStart && it.dateMillis < prevEnd }
    val income = prevTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents }
    val expense = prevTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents }
    val saved = goalEntries.filter { it.dateMillis >= prevStart && it.dateMillis < prevEnd }.sumOf { it.amountCents }
    val spentByCategory = prevTx
        .filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amountCents } }
    val overBudget = budgets.filter { (spentByCategory[it.category] ?: 0L) > it.monthlyLimitCents }

    if (prevTx.isEmpty() && saved == 0L) return

    SectionCard {
        SectionHeader(title = stringResource(R.string.summary_title, monthLabel(prevStart)))
        Text(stringResource(R.string.summary_in, formatMoney(income)), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.summary_out, formatMoney(expense)), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.summary_net, formatMoney(income - expense)), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.summary_saved, formatMoney(saved)), style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (overBudget.isEmpty()) {
                Text(stringResource(R.string.summary_within_budgets), style = MaterialTheme.typography.bodySmall)
            } else {
                Text(stringResource(R.string.summary_over_budgets), style = MaterialTheme.typography.bodySmall)
                overBudget.forEach { budget ->
                    val spent = spentByCategory[budget.category] ?: 0L
                    Text(
                        stringResource(
                            R.string.summary_over_item,
                            budget.category,
                            formatMoney(spent - budget.monthlyLimitCents),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
