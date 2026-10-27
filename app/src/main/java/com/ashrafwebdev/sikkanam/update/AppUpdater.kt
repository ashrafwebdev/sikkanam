package com.ashrafwebdev.sikkanam.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASES_URL = "https://api.github.com/repos/ashrafwebdev/sikkanam/releases/latest"
private const val PREFS = "sikkanam_prefs"
private const val KEY_AUTO_CHECK = "auto_update_check"

/** A newer build found on GitHub. [versionCode] is the build number; [apkUrl] downloads it. */
data class AvailableUpdate(val versionCode: Long, val apkUrl: String)

fun isAutoUpdateCheckOn(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_CHECK, false)

fun setAutoUpdateCheck(context: Context, on: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_AUTO_CHECK, on).apply()
}

/** The build number of the copy that is installed now. */
fun installedVersionCode(context: Context): Long {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
}

/**
 * Asks GitHub for the latest release. Returns it only when its build number is higher than the
 * installed one. Returns null when up to date or when the check fails (offline, rate limit).
 */
suspend fun checkForUpdate(context: Context): AvailableUpdate? = withContext(Dispatchers.IO) {
    try {
        val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
        val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val build = release.getString("tag_name").removePrefix("build-").toLongOrNull() ?: return@withContext null
        if (build <= installedVersionCode(context)) return@withContext null

        val assets = release.getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").endsWith(".apk")) {
                return@withContext AvailableUpdate(build, asset.getString("browser_download_url"))
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

/**
 * Downloads the update and opens Android's install prompt. The user still has to confirm it,
 * because Android doesn't allow apps to install silently. Returns false if the download fails.
 */
suspend fun downloadAndInstall(context: Context, update: AvailableUpdate): Boolean = withContext(Dispatchers.IO) {
    try {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(dir, "sikkanam-update.apk")
        val connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 60_000
        }
        connection.inputStream.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        withContext(Dispatchers.Main) { context.startActivity(intent) }
        true
    } catch (e: Exception) {
        false
    }
}

/** Whether this app is allowed to start installs. Android asks the user to allow it the first time. */
fun canInstallUpdates(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
