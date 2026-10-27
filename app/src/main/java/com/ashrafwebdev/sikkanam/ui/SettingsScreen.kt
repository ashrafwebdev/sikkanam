package com.ashrafwebdev.sikkanam.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrafwebdev.sikkanam.BuildConfig
import com.ashrafwebdev.sikkanam.R
import com.ashrafwebdev.sikkanam.data.TransactionType
import com.ashrafwebdev.sikkanam.payments.isNotificationAccessGranted
import com.ashrafwebdev.sikkanam.payments.isPaymentCaptureOn
import com.ashrafwebdev.sikkanam.payments.setPaymentCaptureOn
import com.ashrafwebdev.sikkanam.reminders.canNotify
import com.ashrafwebdev.sikkanam.reminders.setZakatDueDate
import com.ashrafwebdev.sikkanam.reminders.zakatDueDate
import com.ashrafwebdev.sikkanam.update.UpdateStatus
import com.ashrafwebdev.sikkanam.update.checkForUpdate
import com.ashrafwebdev.sikkanam.update.isAutoUpdateCheckOn
import com.ashrafwebdev.sikkanam.update.setAutoUpdateCheck
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: FinanceViewModel, modifier: Modifier = Modifier) {
    val categories by vm.categories.collectAsState()
    var newName by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var error by remember { mutableStateOf<String?>(null) }
    val categoryError = stringResource(R.string.settings_category_error)

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SectionHeader(title = stringResource(R.string.settings_categories_title))
            Text(
                stringResource(R.string.settings_categories_desc),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        item {
            SectionCard {
                Text(
                    stringResource(R.string.settings_add_category_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = newType == TransactionType.EXPENSE,
                        onClick = { newType = TransactionType.EXPENSE },
                        label = { Text(stringResource(R.string.type_expense)) },
                    )
                    FilterChip(
                        selected = newType == TransactionType.INCOME,
                        onClick = { newType = TransactionType.INCOME },
                        label = { Text(stringResource(R.string.type_income)) },
                    )
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.settings_category_name)) },
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
                            error = categoryError
                        }
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_add))
                }
            }
        }

        item {
            CategoryTypeSection(
                title = stringResource(R.string.settings_expense_categories),
                type = TransactionType.EXPENSE,
                list = categories.filter { it.type == TransactionType.EXPENSE },
                vm = vm,
            )
        }
        item {
            CategoryTypeSection(
                title = stringResource(R.string.settings_income_categories),
                type = TransactionType.INCOME,
                list = categories.filter { it.type == TransactionType.INCOME },
                vm = vm,
            )
        }

        item { LanguageSection() }
        item { TextSizeSection() }
        item { SecuritySection() }
        if (BuildConfig.PAYMENT_ALERTS) item { PaymentSection() }
        item { ReminderSection() }
        if (BuildConfig.SELF_UPDATE) item { UpdateSection() }
        item { BackupSection(vm) }
        item { FeedbackSection() }
        if (BuildConfig.SUPPORT_CARD) item { SupportSection() }
        item { AboutSection() }
        item { Spacer(Modifier.height(16.dp)) }
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
    list: List<com.ashrafwebdev.sikkanam.data.Category>,
    vm: FinanceViewModel,
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var openGroups by remember { mutableStateOf(setOf<String>()) }
    var renaming by remember { mutableStateOf<String?>(null) }
    val names = list.map { it.name }.filter { it.contains(query.trim(), ignoreCase = true) }
    val groups = groupCategories(names, type)
    val renameError = stringResource(R.string.settings_rename_error)

    renaming?.let { oldName ->
        var newName by remember(oldName) { mutableStateOf(oldName) }
        var error by remember(oldName) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.settings_rename_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.settings_new_name)) },
                        singleLine = true,
                    )
                    Text(
                        stringResource(R.string.settings_rename_info),
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
                        error = renameError
                    }
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.action_cancel)) } },
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
                    contentDescription = if (open) stringResource(R.string.settings_collapse) else stringResource(R.string.settings_expand),
                )
            }
        }
        if (open) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.settings_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (groups.isEmpty()) {
                Text(stringResource(R.string.settings_no_matches), style = MaterialTheme.typography.bodySmall)
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
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.settings_rename_title))
                            }
                            IconButton(onClick = { vm.deleteCategory(name, type) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.settings_delete_category))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Chooses the app's language: the phone's language, English, or Tamil. The change applies straight away. */
@Composable
private fun LanguageSection() {
    val context = LocalContext.current
    var language by remember { mutableStateOf(AppLanguageSetting.read(context)) }

    SectionCard {
        SectionHeader(title = stringResource(R.string.settings_language_title))
        Text(
            stringResource(R.string.settings_language_desc),
            style = MaterialTheme.typography.bodySmall,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppLanguage.entries.forEach { option ->
                FilterChip(
                    selected = language == option,
                    onClick = {
                        language = option
                        AppLanguageSetting.save(context, option)
                        (context as? Activity)?.recreate()
                    },
                    label = {
                        Text(
                            when (option) {
                                AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                                AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
                                AppLanguage.TAMIL -> stringResource(R.string.settings_language_tamil)
                            },
                        )
                    },
                )
            }
        }
    }
}

/** Makes the app's text bigger or smaller, on top of the phone's own font size. */
@Composable
private fun TextSizeSection() {
    val context = LocalContext.current
    var textSize by remember { mutableStateOf(TextSizeSetting.read(context)) }

    SectionCard {
        SectionHeader(title = stringResource(R.string.settings_text_size_title))
        Text(
            stringResource(R.string.settings_text_size_desc),
            style = MaterialTheme.typography.bodySmall,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextSize.entries.forEach { option ->
                FilterChip(
                    selected = textSize == option,
                    onClick = {
                        textSize = option
                        TextSizeSetting.save(context, option)
                        // The size is read when the activity starts, so restart it to apply the change.
                        (context as? Activity)?.recreate()
                    },
                    label = { Text(option.label) },
                )
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
        SectionHeader(title = stringResource(R.string.settings_security_title))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_lock_app), style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (available) stringResource(R.string.settings_lock_on)
                    else stringResource(R.string.settings_lock_unavailable),
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

/**
 * Asks users to support development, with the UPI code. Shown in the GitHub build only. The QR image
 * lives in the GitHub build's resources, so it is looked up by name and skipped if it is missing.
 */
@Composable
private fun SupportSection() {
    val context = LocalContext.current
    val qrId = context.resources.getIdentifier("upi_qr", "drawable", context.packageName)
    SectionCard {
        SectionHeader(title = stringResource(R.string.support_title))
        Text(stringResource(R.string.support_message_en), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.support_message_ta), style = MaterialTheme.typography.bodySmall)
        if (qrId != 0) {
            Image(
                painter = painterResource(qrId),
                contentDescription = stringResource(R.string.support_qr_desc),
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }
        Text(stringResource(R.string.support_upi_id), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Who made the app, and where to find them. */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    SectionCard {
        SectionHeader(title = stringResource(R.string.settings_about_title))
        Text(stringResource(R.string.settings_developed_by), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.settings_license), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://smatechsolution.com")))
        }) { Text(stringResource(R.string.settings_visit_site)) }
    }
}

/**
 * Optional: suggests payments from Google Pay and PhonePe notifications. Off by default. The user
 * must turn it on here and allow notification access in phone settings. Nothing is added without a tap.
 */
@Composable
private fun PaymentSection() {
    val context = LocalContext.current
    var on by remember { mutableStateOf(isPaymentCaptureOn(context)) }
    var access by remember { mutableStateOf(isNotificationAccessGranted(context)) }

    SectionCard {
        SectionHeader(title = stringResource(R.string.settings_payment_title))
        Text(
            stringResource(R.string.settings_payment_desc),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_payment_toggle), style = MaterialTheme.typography.bodyLarge)
                Text(
                    when {
                        !on -> stringResource(R.string.settings_payment_off)
                        access -> stringResource(R.string.settings_payment_access_on)
                        else -> stringResource(R.string.settings_payment_access_off)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = on,
                onCheckedChange = {
                    on = it
                    setPaymentCaptureOn(context, it)
                    if (it && !isNotificationAccessGranted(context)) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                },
            )
        }
        OutlinedButton(onClick = {
            access = isNotificationAccessGranted(context)
        }) { Text(stringResource(R.string.settings_check_access)) }
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }) { Text(stringResource(R.string.settings_open_access)) }
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
    val latestText = stringResource(R.string.settings_latest_build)

    SectionCard {
        SectionHeader(title = stringResource(R.string.settings_updates_title))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_auto_update), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.settings_auto_update_desc), style = MaterialTheme.typography.bodySmall)
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
                    message = if (found != null) context.getString(R.string.settings_update_found, found.versionCode)
                    else latestText
                }
            }) { Text(stringResource(R.string.settings_check_now)) }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (update != null) {
            Text(stringResource(R.string.settings_update_available), style = MaterialTheme.typography.bodySmall)
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
    val dateError = stringResource(R.string.settings_date_error)
    val savedReminder = stringResource(R.string.settings_saved_reminder)
    val savedNoNotify = stringResource(R.string.settings_saved_no_notify)
    val removed = stringResource(R.string.settings_zakat_removed)

    SectionCard {
        SectionHeader(title = stringResource(R.string.settings_reminders_title))
        Text(
            stringResource(R.string.settings_reminders_desc),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                message = null
            },
            label = { Text(stringResource(R.string.settings_zakat_date)) },
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
                    message = dateError
                } else {
                    // The parser reads a date only, so its time is already midnight local time.
                    setZakatDueDate(context, parsed.time)
                    message = if (canNotify(context)) savedReminder else savedNoNotify
                }
            }) { Text(stringResource(R.string.action_save)) }
            if (saved != 0L) {
                OutlinedButton(onClick = {
                    setZakatDueDate(context, 0L)
                    text = ""
                    message = removed
                }) { Text(stringResource(R.string.settings_clear)) }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun BackupSection(vm: FinanceViewModel) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    val savedText = stringResource(R.string.settings_backup_saved)
    val startedText = stringResource(R.string.settings_restore_started)
    val invalidText = stringResource(R.string.settings_restore_invalid)

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(vm.backupText()) }
            status = savedText
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            status = if (text != null && vm.restoreBackup(text)) startedText else invalidText
        }
    }

    SectionCard {
        SectionHeader(title = stringResource(R.string.settings_backup_title))
        Text(
            stringResource(R.string.settings_backup_desc),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { saveLauncher.launch("sikkanam-backup.json") }) {
                Text(stringResource(R.string.settings_save_backup))
            }
            OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                Text(stringResource(R.string.settings_restore))
            }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
