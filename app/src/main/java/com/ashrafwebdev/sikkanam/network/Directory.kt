package com.ashrafwebdev.sikkanam.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The service directory (electricians, plumbers, housekeepers, businesses) is published by the
 * developer as one JSON file, and the app downloads it. Shape of the file:
 *
 * {"categories":[{"name":"Electricians","entries":[{"name":"...","area":"...","phone":"...",
 *   "description":"...","languages":["Tamil","English"]}]}]}
 */
const val DIRECTORY_URL = "https://smatechsolution.com/sikkanam/directory.json"

private const val FILE_NAME = "directory.json"

data class DirectoryEntry(
    val name: String,
    val area: String,
    val phone: String,
    val description: String,
    val languages: List<String>,
)

data class DirectoryCategory(val name: String, val entries: List<DirectoryEntry>)

/** The saved copy of the directory, or an empty list if it has not been downloaded yet. */
fun loadDirectory(context: Context): List<DirectoryCategory> {
    val file = File(context.filesDir, FILE_NAME)
    if (!file.exists()) return emptyList()
    return try {
        parseDirectory(file.readText())
    } catch (e: JSONException) {
        emptyList()
    }
}

/**
 * Downloads the directory and saves it. The text is checked before the saved copy is replaced,
 * so a bad download never removes the listings already on the phone. Returns an error message, or null.
 */
suspend fun refreshDirectory(context: Context, url: String = DIRECTORY_URL): String? =
    withContext(Dispatchers.IO) {
        val text = try {
            fetchDirectoryText(url)
        } catch (e: Exception) {
            return@withContext "Could not download the directory. Check your internet connection."
        }
        try {
            parseDirectory(text)
        } catch (e: JSONException) {
            return@withContext "The directory could not be read."
        }
        File(context.filesDir, FILE_NAME).writeText(text)
        null
    }

private fun fetchDirectoryText(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun parseDirectory(text: String): List<DirectoryCategory> {
    val categories = JSONObject(text).getJSONArray("categories")
    return List(categories.length()) { i ->
        val cat = categories.getJSONObject(i)
        DirectoryCategory(
            name = cat.getString("name"),
            entries = cat.optJSONArray("entries")?.let { parseEntries(it) }.orEmpty(),
        )
    }
}

private fun parseEntries(array: JSONArray): List<DirectoryEntry> =
    List(array.length()) { i ->
        val o = array.getJSONObject(i)
        val languages = o.optJSONArray("languages")?.let { arr ->
            List(arr.length()) { arr.getString(it) }
        }.orEmpty()
        DirectoryEntry(
            name = o.getString("name"),
            area = o.optString("area", ""),
            phone = o.optString("phone", ""),
            description = o.optString("description", ""),
            languages = languages,
        )
    }
