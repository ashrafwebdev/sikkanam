package com.ashrafwebdev.sikkanam.data

import org.json.JSONArray
import org.json.JSONObject

/** Everything the app stores, as one object. */
data class BackupData(
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val donations: List<Donation>,
    val categories: List<Category>,
)

private const val FORMAT_VERSION = 1

fun BackupData.toJson(): String {
    val root = JSONObject()
    root.put("format", "sikkanam-backup")
    root.put("version", FORMAT_VERSION)

    root.put("transactions", JSONArray().apply {
        transactions.forEach { t ->
            put(JSONObject().apply {
                // The id is kept so that copies of a recurring template still point at it after a restore.
                put("id", t.id)
                put("type", t.type.name)
                put("amountCents", t.amountCents)
                put("category", t.category)
                put("note", t.note)
                put("dateMillis", t.dateMillis)
                put("halalStatus", t.halalStatus.name)
                put("isRiba", t.isRiba)
                put("recurringMonthly", t.recurringMonthly)
                if (t.sourceId != null) put("sourceId", t.sourceId) else put("sourceId", JSONObject.NULL)
            })
        }
    })
    root.put("budgets", JSONArray().apply {
        budgets.forEach { b ->
            put(JSONObject().apply {
                put("category", b.category)
                put("monthlyLimitCents", b.monthlyLimitCents)
            })
        }
    })
    root.put("donations", JSONArray().apply {
        donations.forEach { d ->
            put(JSONObject().apply {
                put("kind", d.kind.name)
                put("amountCents", d.amountCents)
                put("note", d.note)
                put("dateMillis", d.dateMillis)
            })
        }
    })
    root.put("categories", JSONArray().apply {
        categories.forEach { c ->
            put(JSONObject().apply {
                put("name", c.name)
                put("type", c.type.name)
            })
        }
    })
    return root.toString(2)
}

/** Reads a backup file. Throws if the text isn't a Sikkanam backup. */
fun parseBackup(text: String): BackupData {
    val root = JSONObject(text)
    require(root.optString("format") == "sikkanam-backup") { "This file is not a Sikkanam backup." }

    val transactions = root.getJSONArray("transactions").let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Transaction(
                id = o.optLong("id", 0),
                type = TransactionType.valueOf(o.getString("type")),
                amountCents = o.getLong("amountCents"),
                category = o.getString("category"),
                note = o.optString("note"),
                dateMillis = o.getLong("dateMillis"),
                halalStatus = HalalStatus.valueOf(o.optString("halalStatus", "NONE")),
                isRiba = o.optBoolean("isRiba", false),
                recurringMonthly = o.optBoolean("recurringMonthly", false),
                sourceId = if (o.isNull("sourceId")) null else o.getLong("sourceId"),
            )
        }
    }
    val budgets = root.getJSONArray("budgets").let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Budget(category = o.getString("category"), monthlyLimitCents = o.getLong("monthlyLimitCents"))
        }
    }
    val donations = root.getJSONArray("donations").let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Donation(
                kind = DonationKind.valueOf(o.getString("kind")),
                amountCents = o.getLong("amountCents"),
                note = o.optString("note"),
                dateMillis = o.getLong("dateMillis"),
            )
        }
    }
    val categories = root.getJSONArray("categories").let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Category(name = o.getString("name"), type = TransactionType.valueOf(o.getString("type")))
        }
    }
    return BackupData(transactions, budgets, donations, categories)
}
