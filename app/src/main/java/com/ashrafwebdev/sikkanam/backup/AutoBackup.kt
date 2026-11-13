package com.ashrafwebdev.sikkanam.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.room.withTransaction
import com.ashrafwebdev.sikkanam.data.AppDatabase
import com.ashrafwebdev.sikkanam.data.BackupData
import com.ashrafwebdev.sikkanam.data.toJson
import com.ashrafwebdev.sikkanam.ui.markBackupSaved
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val PREFS = "auto_backup"
private const val KEY_URI = "file_uri"
private const val KEY_LAST_MILLIS = "last_millis"
private const val KEY_LAST_FAILED = "last_failed"
private const val KEY_LOW_STORAGE = "low_storage"
private const val PHONE_COPY_DIR = "auto_backups"
private const val PHONE_COPIES_KEPT = 7
/** Room left over after a write, so the phone never fills up completely. */
private const val SPARE_BYTES = 20L * 1024 * 1024
private const val DAILY_WORK = "sikkanam_daily_backup"
private const val NOW_WORK = "sikkanam_backup_now"

/**
 * Daily automatic backup into one file the user picked with Android's file picker, usually in
 * Google Drive. Drive's own app uploads the file, so Sikkanam needs no Google sign-in or internet
 * permission for this. Each run replaces the file with a fresh copy of all the data.
 */
object AutoBackup {
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun fileUri(context: Context): Uri? = prefs(context).getString(KEY_URI, null)?.let(Uri::parse)

    fun isOn(context: Context): Boolean = fileUri(context) != null

    fun lastMillis(context: Context): Long = prefs(context).getLong(KEY_LAST_MILLIS, 0L)

    fun lastFailed(context: Context): Boolean = prefs(context).getBoolean(KEY_LAST_FAILED, false)

    /** True when the last run skipped the Drive file because the phone was low on storage. */
    fun lowStorage(context: Context): Boolean = prefs(context).getBoolean(KEY_LOW_STORAGE, false)

    /** Daily copies kept inside the app, newest first, so a failed Drive save never leaves no copy at all. */
    fun phoneCopies(context: Context): List<File> =
        File(context.filesDir, PHONE_COPY_DIR).listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.name }
            .orEmpty()

    /** Saves today's copy (replacing an earlier one from today) and drops all but the newest few. */
    internal fun savePhoneCopy(context: Context, json: String): Boolean {
        val dir = File(context.filesDir, PHONE_COPY_DIR).apply { mkdirs() }
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val target = File(dir, "sikkanam-$day.json")
        val temp = File(dir, "${target.name}.tmp")
        val ok = try {
            temp.writeText(json)
            temp.renameTo(target)
        } catch (e: Exception) {
            false
        }
        temp.delete()
        phoneCopies(context).drop(PHONE_COPIES_KEPT).forEach { it.delete() }
        return ok
    }

    /** Whether there is room for [bytes] more, with some to spare. The Drive app caches the file before upload. */
    internal fun hasRoomFor(context: Context, bytes: Long): Boolean = context.filesDir.usableSpace >= bytes * 2 + SPARE_BYTES

    /** Keeps access to the picked file across restarts, then backs up now and every day after. */
    fun turnOn(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // Some file apps don't offer lasting access. The first backup still runs; later ones report a failure.
        }
        prefs(context).edit().putString(KEY_URI, uri.toString()).putBoolean(KEY_LAST_FAILED, false).apply()
        schedule(context)
        backUpNow(context)
    }

    fun turnOff(context: Context) {
        val uri = fileUri(context)
        prefs(context).edit().remove(KEY_URI).apply()
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_WORK)
        if (uri != null) {
            try {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (e: SecurityException) {
                // Access was already gone.
            }
        }
    }

    /** Safe to call on every launch; an existing schedule is kept. Does nothing while turned off. */
    fun schedule(context: Context) {
        if (!isOn(context)) return
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun backUpNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOW_WORK, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AutoBackupWorker>().build())
    }

    /** The picked file's name as the file app shows it, such as "sikkanam-auto-backup.json". */
    fun fileName(context: Context): String? {
        val uri = fileUri(context) ?: return null
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Calls [onChange] whenever the backup status changes, until the returned function is called. */
    fun listen(context: Context, onChange: () -> Unit): () -> Unit {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> onChange() }
        val prefs = prefs(context)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    internal fun recordResult(context: Context, ok: Boolean, lowStorage: Boolean = false) {
        val edit = prefs(context).edit().putBoolean(KEY_LAST_FAILED, !ok).putBoolean(KEY_LOW_STORAGE, lowStorage)
        if (ok) edit.putLong(KEY_LAST_MILLIS, System.currentTimeMillis())
        edit.apply()
        if (ok) markBackupSaved(context)
    }
}

/** Writes a full backup into the chosen file. */
class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        // The daily and "back up now" runs can overlap; only one may write the file at a time.
        return writeLock.withLock {
            val uri = AutoBackup.fileUri(context) ?: return Result.success()
            val db = AppDatabase.create(context)
            val json = try {
                loadBackupData(db).toJson()
            } finally {
                db.close()
            }
            AutoBackup.savePhoneCopy(context, json)
            // Opening the Drive file empties it, so a write that runs out of space part way would leave it blank.
            if (!AutoBackup.hasRoomFor(context, json.toByteArray().size.toLong())) {
                AutoBackup.recordResult(context, ok = false, lowStorage = true)
                return Result.retry()
            }
            val ok = writeText(context, uri, json)
            AutoBackup.recordResult(context, ok)
            if (ok) Result.success() else Result.retry()
        }
    }

    private companion object {
        val writeLock = Mutex()
    }

    /** "wt" empties the file before writing. Not every file app accepts it, so plain "w" is the fallback. */
    private fun writeText(context: Context, uri: Uri, text: String): Boolean {
        for (mode in listOf("wt", "w")) {
            try {
                val stream = context.contentResolver.openOutputStream(uri, mode) ?: continue
                stream.bufferedWriter().use { it.write(text) }
                return true
            } catch (e: Exception) {
                // Try the next mode, or report the failure.
            }
        }
        return false
    }
}

/** Reads everything from the database in one transaction, so related tables match each other. */
suspend fun loadBackupData(db: AppDatabase): BackupData = db.withTransaction { BackupData(
    transactions = db.transactionDao().observeAll().first(),
    budgets = db.budgetDao().observeAll().first(),
    donations = db.donationDao().observeAll().first(),
    categories = db.categoryDao().observeAll().first(),
    goals = db.savingsGoalDao().observeAll().first(),
    tripGroups = db.tripDao().observeGroups().first(),
    tripMembers = db.tripDao().observeMembers().first(),
    tripContributions = db.tripDao().observeContributions().first(),
    tripExpenses = db.tripDao().observeExpenses().first(),
    tripSettledMonths = db.tripDao().observeSettledMonths().first(),
    accounts = db.accountDao().observeAll().first(),
    transfers = db.transferDao().observeAll().first(),
    meetings = db.meetingDao().observeAll().first(),
    dutyAssignments = db.dutyDao().observeAll().first(),
    dutyTypes = db.dutyTypeDao().observeAll().first(),
    goalEntries = db.goalEntryDao().observeAll().first(),
    places = db.placeDao().observeAll().first(),
    debts = db.debtDao().observeAll().first(),
    amaalChecks = db.amaalDao().observeAll().first(),
    khurooj = db.khuroojDao().observeAll().first(),
    notes = db.noteDao().observeAll().first(),
    travelBookings = db.travelDao().observeAll().first(),
    people = db.personDao().observeAll().first(),
    mastooratTrips = db.mastooratDao().observeTrips().first(),
    mastooratPairs = db.mastooratDao().observePairs().first(),
    mastooratTaleem = db.mastooratDao().observeTaleem().first(),
    mastooratTashkeel = db.mastooratDao().observeTashkeel().first(),
    dawatContacts = db.dawatDao().observeAll().first(),
    localityPeople = db.localityDao().observeAll().first(),
    meetingVisits = db.meetingVisitDao().observeAll().first(),
) }
