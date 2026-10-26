package com.ashrafwebdev.sikkanam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.Category
import com.ashrafwebdev.sikkanam.data.TransactionType

@Composable
fun SettingsScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    val categories by vm.categories.collectAsState()
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var error by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Categories", style = MaterialTheme.typography.titleLarge)
            Text(
                "Categories are the master list used when adding transactions and budgets. " +
                    "Deleting a category does not change existing entries.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add a category", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newType == TransactionType.EXPENSE,
                            onClick = { newType = TransactionType.EXPENSE },
                            label = { Text("Expense") },
                        )
                        FilterChip(
                            selected = newType == TransactionType.INCOME,
                            onClick = { newType = TransactionType.INCOME },
                            label = { Text("Income") },
                        )
                    }
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            error = null
                        },
                        label = { Text("Category name") },
                        singleLine = true,
                        isError = error != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    FilledTonalButton(
                        onClick = {
                            if (vm.addCategory(newName, newType)) {
                                newName = ""
                                error = null
                            } else {
                                error = "Enter a name up to 30 characters that isn't already in this list."
                            }
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add")
                    }
                }
            }
        }

        section("Expense categories", categories.filter { it.type == TransactionType.EXPENSE }, vm)
        section("Income categories", categories.filter { it.type == TransactionType.INCOME }, vm)
    }
}

private fun LazyListScope.section(
    title: String,
    list: List<Category>,
    vm: FinanceViewModel,
) {
    item {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
    items(list, key = { "${it.type}-${it.name}" }) { category ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.deleteCategory(category.name, category.type) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete category")
                }
            }
        }
    }
}
