package com.ashrafwebdev.sikkanam.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.clickable
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.data.Category
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.ashrafwebdev.sikkanam.reminders.canNotify
import com.ashrafwebdev.sikkanam.update.UpdateStatus
import com.ashrafwebdev.sikkanam.update.checkForUpdate
import com.ashrafwebdev.sikkanam.update.isAutoUpdateCheckOn
import com.ashrafwebdev.sikkanam.update.setAutoUpdateCheck
import com.ashrafwebdev.sikkanam.reminders.setZakatDueDate
import com.ashrafwebdev.sikkanam.reminders.zakatDueDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
            SectionHeader(title = "Categories")
            Text(
                "The master list used in transactions and budgets. Deleting a category keeps existing entries.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        item {
            SectionCard {
                Text("Add a category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        item {
            CategoryTypeSection(
                title = "Expense categories",
                type = TransactionType.EXPENSE,
                list = categories.filter { it.type == TransactionType.EXPENSE },
                vm = vm,
            )
        }
        item {
            CategoryTypeSection(
                title = "Income categories",
                type = TransactionType.INCOME,
                list = categories.filter { it.type == TransactionType.INCOME },
                vm = vm,
            )
        }

        item { SecuritySection() }
        item { ReminderSection() }
        item { UpdateSection() }
        item { BackupSection(vm) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun LazyListScope.section(
    title: String,
    list: List<Category>,
    vm: FinanceViewModel,
) {
    item {
        SectionHeader(title = title)
    }
    if (list.isEmpty()) {
        item { EmptyState("None yet.") }
    }
    // Compact rows: a long category list stays short on screen.
    items(list, key = { "${it.type}-${it.name}" }) { category ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(category.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.deleteCategory(category.name, category.type) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete category")
            }
        }
    }
}

/**
 * One category type (expense or income), with a count, a search box, and theme groups that
 * open and close. Groups start closed, so a long list stays short on screen.
 */
@Composable
private fun CategoryTypeSection(
    title: String,
    type: TransactionType,
    list: List<Category>,
    vm: FinanceViewModel,
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var openGroups by remember { mutableStateOf(setOf<String>()) }
    var renaming by remember { mutableStateOf<String?>(null) }
    val names = list.map { it.name }.filter { it.contains(query.trim(), ignoreCase = true) }
    val groups = groupCategories(names, type)

    renaming?.let { oldName ->
        var newName by remember(oldName) { mutableStateOf(oldName) }
        var error by remember(oldName) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New name") },
                        singleLine = true,
                    )
                    Text(
                        "Existing entries and budgets with this category are updated too.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (vm.renameCategory(oldName, newName, type)) {
                        renaming = null
                    } else {
                        error = "Enter a unique name up to 30 characters."
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$title (${list.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).clickable { open = !open },
            )
            IconButton(onClick = { open = !open }) {
                Icon(
                    if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (open) "Collapse" else "Expand",
                )
            }
        }
        if (open) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (groups.isEmpty()) {
                Text("No matches.", style = MaterialTheme.typography.bodySmall)
            }
            groups.forEach { (group, groupNames) ->
                val expanded = group in openGroups || query.isNotBlank()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$group (${groupNames.size})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).clickable {
                            openGroups = if (group in openGroups) openGroups - group else openGroups + group
                        },
                    )
                }
                if (expanded) {
                    groupNames.forEach { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { renaming = name }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename category")
                            }
                            IconButton(onClick = { vm.deleteCategory(name, type) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete category")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecuritySection() {
    val context = LocalContext.current
    var locked by remember { mutableStateOf(isLockEnabled(context)) }
    val available = canUseLock(context)

    SectionCard {
        SectionHeader(title = "Security")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Lock app", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (available) "Ask for your fingerprint or screen lock when opening the app."
                    else "Set up a fingerprint or screen lock on this phone to use this.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = locked,
                enabled = available,
                onCheckedChange = {
                    locked = it
                    setLockEnabled(context, it)
                },
            )
        }
    }
}

/** Checks GitHub for a newer build. Off by default, so the app makes no network calls unless turned on. */
@Composable
private fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var autoCheck by remember { mutableStateOf(isAutoUpdateCheckOn(context)) }
    val update by UpdateStatus.available.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }

    SectionCard {
        SectionHeader(title = "Updates")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Check for updates automatically", style = MaterialTheme.typography.bodyLarge)
                Text("Uses the internet only to look for a newer build when the app opens.", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = autoCheck,
                onCheckedChange = {
                    autoCheck = it
                    setAutoUpdateCheck(context, it)
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = {
                scope.launch {
                    val found = checkForUpdate(context)
                    UpdateStatus.set(found)
                    message = if (found != null) "Build ${found.versionCode} is available. Use the Install button at the top."
                    else "You have the latest build, or the check could not reach GitHub."
                }
            }) { Text("Check now") }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (update != null) {
            Text("An update is available. Use the Install button at the top.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Sets the date zakat is due. The app reminds you the day before. Enter it as dd/mm/yyyy. */
@Composable
private fun ReminderSection() {
    val context = LocalContext.current
    val saved = zakatDueDate(context)
    var text by remember {
        mutableStateOf(if (saved == 0L) "" else SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(saved)))
    }
    var message by remember { mutableStateOf<String?>(null) }

    SectionCard {
        SectionHeader(title = "Reminders")
        Text(
            "Bills are reminded the day before their due day. Set your zakat date to be reminded too.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                message = null
            },
            label = { Text("Zakat due date (dd/mm/yyyy)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                val parsed = try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }.parse(text.trim())
                } catch (e: Exception) {
                    null
                }
                if (parsed == null) {
                    message = "Enter a date like 25/03/2027."
                } else {
                    // The parser reads a date only, so its time is already midnight local time.
                    setZakatDueDate(context, parsed.time)
                    message = if (canNotify(context)) "Saved. You'll be reminded the day before." else "Saved, but notifications are off."
                }
            }) { Text("Save") }
            if (saved != 0L) {
                OutlinedButton(onClick = {
                    setZakatDueDate(context, 0L)
                    text = ""
                    message = "Zakat reminder removed."
                }) { Text("Clear") }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun BackupSection(vm: FinanceViewModel) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(vm.backupText()) }
            status = "Backup saved."
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            status = if (text != null && vm.restoreBackup(text)) {
                "Restore started. Your data is replaced with the backup."
            } else {
                "That file is not a Sikkanam backup."
            }
        }
    }

    SectionCard {
        SectionHeader(title = "Backup and restore")
        Text(
            "A backup holds all transactions, budgets, payments, goals, and categories. " +
                "Restoring replaces the data on this phone.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { saveLauncher.launch("sikkanam-backup.json") }) {
                Text("Save backup")
            }
            OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                Text("Restore")
            }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
