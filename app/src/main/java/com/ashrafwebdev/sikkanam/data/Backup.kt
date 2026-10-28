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
    val debts: List<Debt> = emptyList(),
    val amaalChecks: List<AmaalCheck> = emptyList(),
    val khurooj: List<Khurooj> = emptyList(),
    val notes: List<Note> = emptyList(),
    val travelBookings: List<TravelBooking> = emptyList(),
    val people: List<Person> = emptyList(),
    val mastooratTrips: List<MastooratTrip> = emptyList(),
    val mastooratPairs: List<MastooratPair> = emptyList(),
    val mastooratTaleem: List<MastooratTaleem> = emptyList(),
    val mastooratTashkeel: List<MastooratTashkeel> = emptyList(),
    val dawatContacts: List<DawatContact> = emptyList(),
    val localityPeople: List<LocalityPerson> = emptyList(),
    val meetingVisits: List<MeetingVisit> = emptyList(),
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
    root.put("amaalChecks", JSONArray().apply {
        amaalChecks.forEach { c ->
            put(JSONObject().apply {
                put("id", c.id)
                put("groupId", c.groupId)
                put("memberId", c.memberId)
                put("dayMillis", c.dayMillis)
                put("amaal", c.amaal)
                put("done", c.done)
            })
        }
    })
    root.put("khurooj", JSONArray().apply {
        khurooj.forEach { k ->
            put(JSONObject().apply {
                put("id", k.id)
                put("groupId", k.groupId)
                put("type", k.type.name)
                put("startMillis", k.startMillis)
                put("endMillis", k.endMillis)
                put("places", k.places)
                put("note", k.note)
            })
        }
    })
    root.put("debts", JSONArray().apply {
        debts.forEach { d ->
            put(JSONObject().apply {
                put("id", d.id)
                put("person", d.person)
                put("direction", d.direction.name)
                put("amountCents", d.amountCents)
                put("paidCents", d.paidCents)
                put("dateMillis", d.dateMillis)
                put("dueMillis", d.dueMillis)
                put("note", d.note)
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
                put("personId", m.personId)
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
                put("groupId", m.groupId)
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
                put("groupId", d.groupId)
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
    root.put("dawatContacts", JSONArray().apply {
        dawatContacts.forEach { d ->
            put(JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
                put("phone", d.phone)
                put("area", d.area)
                put("status", d.status.name)
                put("metMillis", d.metMillis)
                put("followUpMillis", d.followUpMillis)
                put("notes", d.notes)
                put("personId", d.personId)
            })
        }
    })
    root.put("localityPeople", JSONArray().apply {
        localityPeople.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("phone", p.phone)
                put("locality", p.locality)
                put("wentFortyDays", p.wentFortyDays)
                put("wentFourMonths", p.wentFourMonths)
                put("active", p.active)
                put("notes", p.notes)
                put("availableTypes", p.availableTypes)
                put("personId", p.personId)
            })
        }
    })
    root.put("meetingVisits", JSONArray().apply {
        meetingVisits.forEach { v ->
            put(JSONObject().apply {
                put("id", v.id)
                put("personId", v.personId)
                put("dayMillis", v.dayMillis)
                put("note", v.note)
                put("taskeel", v.taskeel)
            })
        }
    })
    root.put("people", JSONArray().apply {
        people.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("phone", p.phone)
                put("address", p.address)
            })
        }
    })
    root.put("mastooratTrips", JSONArray().apply {
        mastooratTrips.forEach { t ->
            put(JSONObject().apply {
                put("id", t.id)
                put("groupId", t.groupId)
                put("effort", t.effort.name)
                put("startMillis", t.startMillis)
                put("endMillis", t.endMillis)
                put("place", t.place)
            })
        }
    })
    root.put("mastooratPairs", JSONArray().apply {
        mastooratPairs.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("tripId", p.tripId)
                put("mahramPersonId", p.mahramPersonId)
                put("childAge", p.childAge)
                put("relation", p.relation.name)
            })
        }
    })
    root.put("mastooratTaleem", JSONArray().apply {
        mastooratTaleem.forEach { t ->
            put(JSONObject().apply {
                put("id", t.id)
                put("groupId", t.groupId)
                put("kind", t.kind.name)
                put("dayMillis", t.dayMillis)
                put("house", t.house)
                put("attended", t.attended)
                put("readerPersonId", t.readerPersonId)
                put("receptionPersonId", t.receptionPersonId)
                put("sixPointsPersonId", t.sixPointsPersonId)
                put("tashkeelPersonId", t.tashkeelPersonId)
            })
        }
    })
    root.put("mastooratTashkeel", JSONArray().apply {
        mastooratTashkeel.forEach { t ->
            put(JSONObject().apply {
                put("id", t.id)
                put("groupId", t.groupId)
                put("dayMillis", t.dayMillis)
                put("kind", t.kind.name)
                put("joined", t.joined)
            })
        }
    })
    root.put("notes", JSONArray().apply {
        notes.forEach { n ->
            put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("body", n.body)
                put("updatedMillis", n.updatedMillis)
                put("pinned", n.pinned)
            })
        }
    })
    root.put("travelBookings", JSONArray().apply {
        travelBookings.forEach { b ->
            put(JSONObject().apply {
                put("id", b.id)
                put("mode", b.mode.name)
                put("operator", b.operator)
                put("fromPlace", b.fromPlace)
                put("toPlace", b.toPlace)
                put("departMillis", b.departMillis)
                put("arriveMillis", b.arriveMillis)
                put("reference", b.reference)
                put("fareCents", b.fareCents)
                put("seat", b.seat)
                put("notes", b.notes)
                put("groupId", b.groupId)
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
    // Amaal checks and khurooj are optional too. Older backups have none.
    val amaalChecks = root.optJSONArray("amaalChecks")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            AmaalCheck(
                id = o.optLong("id", 0),
                groupId = o.getLong("groupId"),
                memberId = o.getLong("memberId"),
                dayMillis = o.getLong("dayMillis"),
                amaal = o.getString("amaal"),
                done = o.getBoolean("done"),
            )
        }
    } ?: emptyList()
    val khurooj = root.optJSONArray("khurooj")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Khurooj(
                id = o.optLong("id", 0),
                groupId = o.getLong("groupId"),
                type = KhuroojType.valueOf(o.optString("type", "OTHER")),
                startMillis = o.getLong("startMillis"),
                endMillis = o.optLong("endMillis", 0),
                places = o.optString("places", ""),
                note = o.optString("note", ""),
            )
        }
    } ?: emptyList()
    // Debts are optional too. Older backups have none.
    val debts = root.optJSONArray("debts")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Debt(
                id = o.optLong("id", 0),
                person = o.getString("person"),
                direction = DebtDirection.valueOf(o.getString("direction")),
                amountCents = o.getLong("amountCents"),
                paidCents = o.optLong("paidCents", 0),
                dateMillis = o.getLong("dateMillis"),
                dueMillis = o.optLong("dueMillis", 0),
                note = o.optString("note", ""),
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
                personId = o.optLong("personId", 0),
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
                groupId = o.optLong("groupId", 0),
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
                groupId = o.optLong("groupId", 0),
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

    // Travel bookings are optional too. Older backups have none.
    val travelBookings = root.optJSONArray("travelBookings")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            TravelBooking(
                id = o.optLong("id", 0),
                mode = TravelMode.valueOf(o.optString("mode", "BUS")),
                operator = o.optString("operator"),
                fromPlace = o.getString("fromPlace"),
                toPlace = o.getString("toPlace"),
                departMillis = o.getLong("departMillis"),
                arriveMillis = o.optLong("arriveMillis", 0),
                reference = o.optString("reference"),
                fareCents = o.optLong("fareCents", 0),
                seat = o.optString("seat"),
                notes = o.optString("notes"),
                groupId = o.optLong("groupId", 0),
            )
        }
    } ?: emptyList()

    // Notes are optional too. Older backups have none.
    val notes = root.optJSONArray("notes")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Note(
                id = o.optLong("id", 0),
                title = o.getString("title"),
                body = o.optString("body", ""),
                updatedMillis = o.optLong("updatedMillis", 0),
                pinned = o.optBoolean("pinned", false),
            )
        }
    } ?: emptyList()

    // Dawat, locality and meeting records are optional. Older backups have none.
    val dawatContacts = root.optJSONArray("dawatContacts")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            DawatContact(
                id = o.optLong("id", 0),
                name = o.getString("name"),
                phone = o.optString("phone"),
                area = o.optString("area"),
                status = DawatStatus.valueOf(o.optString("status", DawatStatus.MET.name)),
                metMillis = o.optLong("metMillis", 0),
                followUpMillis = o.optLong("followUpMillis", 0),
                notes = o.optString("notes"),
                personId = o.optLong("personId", 0),
            )
        }
    } ?: emptyList()
    val localityPeople = root.optJSONArray("localityPeople")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            LocalityPerson(
                id = o.optLong("id", 0),
                name = o.getString("name"),
                phone = o.optString("phone"),
                locality = o.optString("locality"),
                wentFortyDays = o.optBoolean("wentFortyDays", false),
                wentFourMonths = o.optBoolean("wentFourMonths", false),
                active = o.optBoolean("active", false),
                notes = o.optString("notes"),
                availableTypes = o.optString("availableTypes"),
                personId = o.optLong("personId", 0),
            )
        }
    } ?: emptyList()
    val meetingVisits = root.optJSONArray("meetingVisits")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            MeetingVisit(
                id = o.optLong("id", 0),
                personId = o.getLong("personId"),
                dayMillis = o.optLong("dayMillis", 0),
                note = o.optString("note"),
                taskeel = o.optString("taskeel"),
            )
        }
    } ?: emptyList()

    // The master register and Mastoorat records are optional too. Older backups have none.
    val people = root.optJSONArray("people")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Person(
                id = o.optLong("id", 0),
                name = o.getString("name"),
                phone = o.optString("phone"),
                address = o.optString("address"),
            )
        }
    } ?: emptyList()
    val mastooratTrips = root.optJSONArray("mastooratTrips")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            MastooratTrip(
                id = o.optLong("id", 0),
                groupId = o.getLong("groupId"),
                effort = MastooratEffort.valueOf(o.getString("effort")),
                startMillis = o.optLong("startMillis", 0),
                endMillis = o.optLong("endMillis", 0),
                place = o.optString("place"),
            )
        }
    } ?: emptyList()
    val mastooratPairs = root.optJSONArray("mastooratPairs")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            MastooratPair(
                id = o.optLong("id", 0),
                tripId = o.getLong("tripId"),
                mahramPersonId = o.optLong("mahramPersonId", 0),
                childAge = o.optInt("childAge", 0),
                relation = MahramRelation.valueOf(o.optString("relation", MahramRelation.OTHER.name)),
            )
        }
    } ?: emptyList()
    val mastooratTaleem = root.optJSONArray("mastooratTaleem")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            MastooratTaleem(
                id = o.optLong("id", 0),
                groupId = o.getLong("groupId"),
                kind = TaleemKind.valueOf(o.getString("kind")),
                dayMillis = o.optLong("dayMillis", 0),
                house = o.optString("house"),
                attended = o.optInt("attended", 0),
                readerPersonId = o.optLong("readerPersonId", 0),
                receptionPersonId = o.optLong("receptionPersonId", 0),
                sixPointsPersonId = o.optLong("sixPointsPersonId", 0),
                tashkeelPersonId = o.optLong("tashkeelPersonId", 0),
            )
        }
    } ?: emptyList()
    val mastooratTashkeel = root.optJSONArray("mastooratTashkeel")?.let { arr ->
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            MastooratTashkeel(
                id = o.optLong("id", 0),
                groupId = o.getLong("groupId"),
                dayMillis = o.optLong("dayMillis", 0),
                kind = TashkeelKind.valueOf(o.getString("kind")),
                joined = o.optInt("joined", 0),
            )
        }
    } ?: emptyList()

    return BackupData(
        transactions, budgets, donations, categories, goals,
        tripGroups, tripMembers, tripContributions, tripExpenses, tripSettledMonths,
        accounts, transfers, meetings, dutyAssignments, dutyTypes,
        goalEntries = goalEntries,
        places = places,
        debts = debts,
        amaalChecks = amaalChecks,
        khurooj = khurooj,
        notes = notes,
        travelBookings = travelBookings,
        people = people,
        mastooratTrips = mastooratTrips,
        mastooratPairs = mastooratPairs,
        mastooratTaleem = mastooratTaleem,
        mastooratTashkeel = mastooratTashkeel,
        dawatContacts = dawatContacts,
        localityPeople = localityPeople,
        meetingVisits = meetingVisits,
    )
}
