package com.ashrafwebdev.sikkanam.data

import org.json.JSONArray
import org.json.JSONObject

/** Everything the app stores, as one object. */
data class BackupData(
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val donations: List<Donation>,
    val categories: List<Category>,
    val goals: List<SavingsGoal> = emptyList(),
    val tripGroups: List<TripGroup> = emptyList(),
    val tripMembers: List<TripMember> = emptyList(),
    val tripContributions: List<TripContribution> = emptyList(),
    val tripExpenses: List<TripExpense> = emptyList(),
    val tripSettledMonths: List<TripSettledMonth> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val transfers: List<Transfer> = emptyList(),
    val meetings: List<Meeting> = emptyList(),
    val dutyAssignments: List<DutyAssignment> = emptyList(),
    val dutyTypes: List<DutyType> = emptyList(),
    val goalEntries: List<GoalEntry> = emptyList(),
    val places: List<Place> = emptyList(),
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
                put("party", t.party)
                put("paymentMethod", t.paymentMethod)
                put("accountId", t.accountId ?: JSONObject.NULL)
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
    root.put("goals", JSONArray().apply {
        goals.forEach { g ->
            put(JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("targetCents", g.targetCents)
                put("savedCents", g.savedCents)
            })
        }
    })
    root.put("goalEntries", JSONArray().apply {
        goalEntries.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id)
                put("goalId", e.goalId)
                put("amountCents", e.amountCents)
                put("dateMillis", e.dateMillis)
                put("note", e.note)
            })
        }
    })
    root.put("places", JSONArray().apply {
        places.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("parentId", p.parentId ?: JSONObject.NULL)
                put("level", p.level.name)
                put("name", p.name)
            })
        }
    })
    root.put("tripGroups", JSONArray().apply {
        tripGroups.forEach { g ->
            put(JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("kind", g.kind.name)
                put("splitByIncome", g.splitByIncome)
            })
        }
    })
    root.put("tripMembers", JSONArray().apply {
        tripMembers.forEach { m ->
            put(JSONObject().apply {
                put("id", m.id)
                put("groupId", m.groupId)
                put("name", m.name)
                put("incomeCents", m.incomeCents)
                put("phone", m.phone)
                put("address", m.address)
                put("days", m.days)
                put("advanceCents", m.advanceCents)
                put("confirmed", m.confirmed)
            })
        }
    })
    root.put("tripContributions", JSONArray().apply {
        tripContributions.forEach { c ->
            put(JSONObject().apply {
                put("id", c.id)
                put("groupId", c.groupId)
                put("memberId", c.memberId)
                put("amountCents", c.amountCents)
                put("dateMillis", c.dateMillis)
            })
        }
    })
    root.put("tripExpenses", JSONArray().apply {
        tripExpenses.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id)
                put("groupId", e.groupId)
                put("participantIds", e.participantIds)
                put("recurringMonthly", e.recurringMonthly)
                put("sourceId", e.sourceId ?: JSONObject.NULL)
                put("paidByMemberId", e.paidByMemberId ?: JSONObject.NULL)
                put("description", e.description)
                put("amountCents", e.amountCents)
                put("dateMillis", e.dateMillis)
            })
        }
    })
    root.put("accounts", JSONArray().apply {
        accounts.forEach { a ->
            put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("openingCents", a.openingCents)
            })
        }
    })
    root.put("transfers", JSONArray().apply {
        transfers.forEach { t ->
            put(JSONObject().apply {
                put("id", t.id)
                put("fromAccountId", t.fromAccountId)
                put("toAccountId", t.toAccountId)
                put("amountCents", t.amountCents)
                put("dateMillis", t.dateMillis)
                put("note", t.note)
            })
        }
    })
    root.put("meetings", JSONArray().apply {
        meetings.forEach { m ->
            put(JSONObject().apply {
                put("id", m.id)
                put("dateMillis", m.dateMillis)
                put("topic", m.topic)
                put("attendees", m.attendees)
                put("notes", m.notes)
            })
        }
    })
    root.put("dutyAssignments", JSONArray().apply {
        dutyAssignments.forEach { d ->
            put(JSONObject().apply {
                put("id", d.id)
                put("dayMillis", d.dayMillis)
                put("duty", d.duty)
                put("personName", d.personName)
                put("done", d.done)
            })
        }
    })
    root.put("dutyTypes", JSONArray().apply {
        dutyTypes.forEach { d ->
            put(JSONObject().apply {
                put("name", d.name)
                put("sortOrder", d.sortOrder)
            })
        }
    })
    root.put("tripSettledMonths", JSONArray().apply {
        tripSettledMonths.forEach { s ->
            put(JSONObject().apply {
                put("groupId", s.groupId)
                put("monthMillis", s.monthMillis)
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
                party = o.optString("party"),
                paymentMethod = o.optString("paymentMethod"),
                accountId = if (o.isNull("accountId")) null else o.getLong("accountId"),
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
    // Older backups have no goals, so an absent list means none.
    val goals = root.optJSONArray("goals")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            SavingsGoal(
                id = o.optLong("id", 0),
                name = o.getString("name"),
                targetCents = o.getLong("targetCents"),
                savedCents = o.getLong("savedCents"),
            )
        }
    } ?: emptyList()
    // Places are optional too. Older backups have none.
    val places = root.optJSONArray("places")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Place(
                id = o.optLong("id", 0),
                parentId = if (o.isNull("parentId")) null else o.getLong("parentId"),
                level = PlaceLevel.valueOf(o.getString("level")),
                name = o.getString("name"),
            )
        }
    } ?: emptyList()
    // Goal history is optional too. Older backups have none.
    val goalEntries = root.optJSONArray("goalEntries")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            GoalEntry(
                id = o.optLong("id", 0),
                goalId = o.getLong("goalId"),
                amountCents = o.getLong("amountCents"),
                dateMillis = o.getLong("dateMillis"),
                note = o.optString("note", ""),
            )
        }
    } ?: emptyList()
    // Trip data is also optional, so older backups still restore.
    val tripGroups = root.optJSONArray("tripGroups")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            TripGroup(
                id = o.optLong("id", 0),
                name = o.getString("name"),
                kind = GroupKind.valueOf(o.optString("kind", "TRIP")),
                splitByIncome = o.optBoolean("splitByIncome", false),
            )
        }
    } ?: emptyList()
    val tripMembers = root.optJSONArray("tripMembers")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            TripMember(
                id = o.optLong("id", 0),
                groupId = o.getLong("groupId"),
                name = o.getString("name"),
                incomeCents = o.optLong("incomeCents", 0),
                phone = o.optString("phone"),
                address = o.optString("address"),
                days = o.optInt("days", 0),
                advanceCents = o.optLong("advanceCents", 0),
                confirmed = o.optBoolean("confirmed", true),
            )
        }
    } ?: emptyList()
    val tripContributions = root.optJSONArray("tripContributions")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            TripContribution(
                id = o.optLong("id", 0),
                groupId = o.getLong("groupId"),
                memberId = o.getLong("memberId"),
                amountCents = o.getLong("amountCents"),
                dateMillis = o.getLong("dateMillis"),
            )
        }
    } ?: emptyList()
    val tripExpenses = root.optJSONArray("tripExpenses")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            TripExpense(
                id = o.optLong("id", 0),
                groupId = o.getLong("groupId"),
                paidByMemberId = if (o.isNull("paidByMemberId")) null else o.getLong("paidByMemberId"),
                description = o.getString("description"),
                amountCents = o.getLong("amountCents"),
                dateMillis = o.getLong("dateMillis"),
                participantIds = o.optString("participantIds"),
                recurringMonthly = o.optBoolean("recurringMonthly", false),
                sourceId = if (o.isNull("sourceId")) null else o.getLong("sourceId"),
            )
        }
    } ?: emptyList()

    val tripSettledMonths = root.optJSONArray("tripSettledMonths")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            TripSettledMonth(groupId = o.getLong("groupId"), monthMillis = o.getLong("monthMillis"))
        }
    } ?: emptyList()

    val accounts = root.optJSONArray("accounts")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Account(id = o.optLong("id", 0), name = o.getString("name"), openingCents = o.getLong("openingCents"))
        }
    } ?: emptyList()
    val transfers = root.optJSONArray("transfers")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Transfer(
                id = o.optLong("id", 0),
                fromAccountId = o.getLong("fromAccountId"),
                toAccountId = o.getLong("toAccountId"),
                amountCents = o.getLong("amountCents"),
                dateMillis = o.getLong("dateMillis"),
                note = o.optString("note"),
            )
        }
    } ?: emptyList()

    val meetings = root.optJSONArray("meetings")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Meeting(
                id = o.optLong("id", 0),
                dateMillis = o.getLong("dateMillis"),
                topic = o.getString("topic"),
                attendees = o.optString("attendees"),
                notes = o.optString("notes"),
            )
        }
    } ?: emptyList()

    val dutyAssignments = root.optJSONArray("dutyAssignments")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            DutyAssignment(
                id = o.optLong("id", 0),
                dayMillis = o.getLong("dayMillis"),
                duty = o.getString("duty"),
                personName = o.getString("personName"),
                done = o.optBoolean("done", false),
            )
        }
    } ?: emptyList()

    val dutyTypes = root.optJSONArray("dutyTypes")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            DutyType(name = o.getString("name"), sortOrder = o.optInt("sortOrder", i))
        }
    } ?: emptyList()

    return BackupData(
        transactions, budgets, donations, categories, goals,
        tripGroups, tripMembers, tripContributions, tripExpenses, tripSettledMonths,
        accounts, transfers, meetings, dutyAssignments, dutyTypes,
        goalEntries = goalEntries,
        places = places,
    )
}
