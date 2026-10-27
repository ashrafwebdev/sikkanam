package com.ashrafwebdev.sikkanam.ui

import com.ashrafwebdev.sikkanam.data.TransactionType

/** Theme groups for the default categories. Custom categories fall into "Custom". */
private val groupOfName: Map<String, String> = buildMap {
    fun add(group: String, vararg names: String) = names.forEach { put(it, group) }
    add("Essentials", "Food", "Groceries", "Transport", "Fuel", "Bills", "Electricity", "Water",
        "Mobile and internet", "Rent", "Household", "Medicine", "Health")
    add("Family and lifestyle", "Family", "Education", "School fees", "Gifts", "Clothing", "Shopping",
        "Eating out", "Travel")
    add("Islamic", "Zakat", "Charity", "Masjid donation", "Waqf", "Hajj and Umrah", "Eid",
        "Ramadan and iftar", "Islamic education", "Books and Quran", "Nikah and wedding", "Aqiqah", "Qurbani")
    add("Savings and other", "Savings", "Other")
    add("Work", "Salary", "Business", "Halal business", "Freelance")
    add("Other income", "Rental income")
}

/** The group a category belongs to, for showing long lists in sections. */
fun categoryGroup(name: String, type: TransactionType): String =
    groupOfName[name] ?: if (type == TransactionType.INCOME) "Other income" else "Custom"

/** Group order as shown on screen. */
private val groupOrder = listOf(
    "Essentials", "Family and lifestyle", "Islamic", "Savings and other", "Work", "Other income", "Custom",
)

/** Groups [names] for [type], in display order. Empty groups are left out. */
fun groupCategories(names: List<String>, type: TransactionType): List<Pair<String, List<String>>> {
    val byGroup = names.groupBy { categoryGroup(it, type) }
    return groupOrder.mapNotNull { group -> byGroup[group]?.let { group to it.sorted() } }
}
