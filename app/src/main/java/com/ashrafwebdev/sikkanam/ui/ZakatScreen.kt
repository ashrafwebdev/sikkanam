package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.zakat.ZakatCalculator
import com.ashrafwebdev.sikkanam.zakat.ZakatInput

@Composable
fun ZakatScreen(modifier: Modifier = Modifier) {
    var cash by remember { mutableStateOf("") }
    var goldGrams by remember { mutableStateOf("") }
    var goldPrice by remember { mutableStateOf("") }
    var silverGrams by remember { mutableStateOf("") }
    var silverPrice by remember { mutableStateOf("") }
    var tradeGoods by remember { mutableStateOf("") }
    var owed by remember { mutableStateOf("") }
    var debts by remember { mutableStateOf("") }
    var useSilver by remember { mutableStateOf(true) }

    val result = ZakatCalculator.calculate(
        ZakatInput(
            cash = cash.toMoney(),
            goldGrams = goldGrams.toMoney(),
            goldPricePerGram = goldPrice.toMoney(),
            silverGrams = silverGrams.toMoney(),
            silverPricePerGram = silverPrice.toMoney(),
            tradeGoods = tradeGoods.toMoney(),
            moneyOwedToYou = owed.toMoney(),
            debtsDue = debts.toMoney(),
            useSilverNisab = useSilver,
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Result first, so the answer is visible without scrolling.
        SectionCard(
            containerColor = if (result.isDue) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                if (result.isDue) "Zakat due" else "No zakat due",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                if (result.isDue) formatMoney(toCents(result.zakat)) else formatMoney(0L),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Net wealth ${formatMoney(toCents(result.wealth))} · nisab ${formatMoney(toCents(result.nisab))}",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        SectionCard {
            SectionHeader(title = "Nisab threshold")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = useSilver, onClick = { useSilver = true }, label = { Text("Silver (595 g)") })
                FilterChip(selected = !useSilver, onClick = { useSilver = false }, label = { Text("Gold (85 g)") })
            }
        }

        SectionCard {
            SectionHeader(title = "Assets")
            MoneyField("Cash and bank balance", cash) { cash = it }
            MoneyField("Gold (grams)", goldGrams) { goldGrams = it }
            MoneyField("Gold price per gram", goldPrice) { goldPrice = it }
            MoneyField("Silver (grams)", silverGrams) { silverGrams = it }
            MoneyField("Silver price per gram", silverPrice) { silverPrice = it }
            MoneyField("Trade goods (stock for sale)", tradeGoods) { tradeGoods = it }
            MoneyField("Money owed to you", owed) { owed = it }
        }

        SectionCard {
            SectionHeader(title = "Debts")
            MoneyField("Debts due now", debts) { debts = it }
        }

        Text(
            "Zakat is 2.5% of wealth above the nisab, held for one lunar year. " +
                "The app doesn't check the lunar year for you. Check your situation with a scholar.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MoneyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun String.toMoney(): Double = trim().toDoubleOrNull() ?: 0.0

private fun toCents(amount: Double): Long = Math.round(amount * 100)
