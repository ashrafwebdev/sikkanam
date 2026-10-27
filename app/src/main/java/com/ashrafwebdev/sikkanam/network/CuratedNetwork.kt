package com.ashrafwebdev.sikkanam.network

import android.content.Context
import com.ashrafwebdev.sikkanam.data.Place
import com.ashrafwebdev.sikkanam.data.PlaceDao
import com.ashrafwebdev.sikkanam.data.PlaceLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * The curated list is published by the developer as one JSON file. The app downloads it, so every
 * user sees the same places. Nothing is sent from the phone. Shape of the file:
 *
 * {"states":[{"name":"...","districts":[{"name":"...","zones":[{"name":"...","halqas":[{"name":"...","masjids":["..."]}]}]}]}]}
 */
const val NETWORK_URL = "https://smatechsolution.com/sikkanam/network.json"

private const val PREFS = "network"
private const val KEY_UPDATED = "updated_millis"

/** When the list was last downloaded, or 0 if never. */
fun lastNetworkUpdate(context: Context): Long =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_UPDATED, 0L)

private class Node(val name: String, val level: PlaceLevel, val children: List<Node>)

/**
 * Downloads the list and replaces the saved copy. The file is read completely before anything is
 * changed, so a bad download never leaves the list half-replaced. Returns an error message, or null on success.
 */
suspend fun refreshCuratedNetwork(context: Context, dao: PlaceDao, url: String = NETWORK_URL): String? =
    withContext(Dispatchers.IO) {
        val text = try {
            fetchText(url)
        } catch (e: Exception) {
            return@withContext "Could not download the list. Check your internet connection."
        }
        val nodes = try {
            parseNodes(JSONObject(text).getJSONArray("states"), PlaceLevel.STATE)
        } catch (e: JSONException) {
            return@withContext "The list could not be read."
        }
        dao.clearAll()
        insertNodes(dao, nodes, parentId = null)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_UPDATED, System.currentTimeMillis()).apply()
        null
    }

private fun fetchText(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

/** Reads the items of one level. Masjids are plain strings; every other level is an object with a name and children. */
private fun parseNodes(array: JSONArray, level: PlaceLevel): List<Node> =
    List(array.length()) { i ->
        val item = array.get(i)
        if (item is String) {
            Node(item.trim(), level, emptyList())
        } else {
            val obj = item as JSONObject
            // A level can be skipped when the list has no name for it, so a district may list
            // halqas directly, and a zone may list masjids directly. The first key present is used.
            val children = when (level) {
                PlaceLevel.STATE -> obj.optJSONArray("districts")?.let { parseNodes(it, PlaceLevel.DISTRICT) }
                    ?: obj.optJSONArray("zones")?.let { parseNodes(it, PlaceLevel.ZONE) }
                PlaceLevel.DISTRICT -> obj.optJSONArray("zones")?.let { parseNodes(it, PlaceLevel.ZONE) }
                    ?: obj.optJSONArray("halqas")?.let { parseNodes(it, PlaceLevel.HALQA) }
                PlaceLevel.ZONE -> obj.optJSONArray("halqas")?.let { parseNodes(it, PlaceLevel.HALQA) }
                    ?: obj.optJSONArray("masjids")?.let { parseNodes(it, PlaceLevel.MASJID) }
                PlaceLevel.HALQA -> obj.optJSONArray("masjids")?.let { parseNodes(it, PlaceLevel.MASJID) }
                PlaceLevel.MASJID -> null
            }.orEmpty()
            Node(obj.getString("name").trim(), level, children)
        }
    }

private suspend fun insertNodes(dao: PlaceDao, nodes: List<Node>, parentId: Long?) {
    nodes.forEach { node ->
        val id = dao.insert(Place(parentId = parentId, level = node.level, name = node.name))
        insertNodes(dao, node.children, id)
    }
}
