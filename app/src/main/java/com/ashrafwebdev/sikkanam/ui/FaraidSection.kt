package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ashrafwebdev.sikkanam.inheritance.Fraction
import com.ashrafwebdev.sikkanam.inheritance.Heirs
import com.ashrafwebdev.sikkanam.inheritance.calculateFaraid

/**
 * Inheritance (faraid) helper. Enter the estate and the heirs; it shows each share.
 * It covers the spouse, parents, sons and daughters only, so check complicated cases with a scholar.
 */
@Composable
fun FaraidSection() {
    var estate by remember { mutableStateOf("") }
    var debts by remember { mutableStateOf("") }
    var husband by remember { mutableStateOf(false) }
    var wives by remember { mutableStateOf("0") }
    var sons by remember { mutableStateOf("0") }
    var daughters by remember { mutableStateOf("0") }
    var father by remember { mutableStateOf(false) }
    var mother by remember { mutableStateOf(false) }

    val heirs = Heirs(
        husband = husband,
        wives = wives.toIntOrNull() ?: 0,
        sons = sons.toIntOrNull() ?: 0,
        daughters = daughters.toIntOrNull() ?: 0,
        father = father,
        mother = mother,
    )
    val anyHeir = heirs.husband || heirs.wives > 0 || heirs.sons > 0 || heirs.daughters > 0 || heirs.father || heirs.mother
    val netCents = (parseMoneyCents(estate) - parseMoneyCents(debts)).coerceAtLeast(0L)
    val shares = if (anyHeir) calculateFaraid(heirs) else emptyList()

    SectionCard {
        SectionHeader(title = stringResource(R.string.faraid_title))
        Text(stringResource(R.string.faraid_desc), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = estate,
            onValueChange = { estate = it },
            label = { Text(stringResource(R.string.faraid_estate)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = debts,
            onValueChange = { debts = it },
            label = { Text(stringResource(R.string.faraid_debts)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        CheckRow(stringResource(R.string.faraid_husband), husband) { husband = it }
        CountField(stringResource(R.string.faraid_wives), wives) { wives = it }
        CountField(stringResource(R.string.faraid_sons), sons) { sons = it }
        CountField(stringResource(R.string.faraid_daughters), daughters) { daughters = it }
        CheckRow(stringResource(R.string.faraid_father), father) { father = it }
        CheckRow(stringResource(R.string.faraid_mother), mother) { mother = it }

        if (anyHeir) {
            Text(stringResource(R.string.faraid_net, formatMoney(netCents)), style = MaterialTheme.typography.bodyMedium)
            shares.forEach { share ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${heirName(share.key)} × ${share.count}: ${fractionText(share.total)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        formatMoney(netCents * share.total.num / share.total.den),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Text(stringResource(R.string.faraid_warning), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun heirName(key: String): String = stringResource(
    when (key) {
        "HUSBAND" -> R.string.faraid_husband
        "WIFE" -> R.string.faraid_wives
        "SON" -> R.string.faraid_sons
        "DAUGHTER", "DAUGHTERS" -> R.string.faraid_daughters
        "FATHER" -> R.string.faraid_father
        else -> R.string.faraid_mother
    },
)

private fun fractionText(f: Fraction): String = "${f.num}/${f.den}"

private fun parseMoneyCents(text: String): Long {
    val value = text.trim().toDoubleOrNull() ?: return 0L
    return Math.round(value * 100)
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CountField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(2)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
