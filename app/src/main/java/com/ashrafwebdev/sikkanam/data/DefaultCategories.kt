package com.ashrafwebdev.sikkanam.data

/** Categories every new install starts with. Users can add or remove their own in Settings. */
object DefaultCategories {
    val expense = listOf(
        "Food", "Groceries", "Eating out", "Transport", "Fuel", "Bills", "Electricity", "Water",
        "Mobile and internet", "Rent", "Household", "Clothing", "Health", "Medicine", "Education",
        "School fees", "Family", "Gifts", "Charity", "Travel", "Shopping",
        "Savings", "Zakat", "Masjid donation", "Waqf", "Hajj and Umrah", "Eid", "Ramadan and iftar",
        "Islamic education", "Books and Quran", "Nikah and wedding", "Aqiqah", "Qurbani", "Other",
    )

    val income = listOf(
        "Salary", "Business", "Halal business", "Freelance", "Rental income", "Gifts", "Other",
    )

    /** Inserts every default category that isn't already present. */
    fun insertInto(execSql: (String, Array<Any>) -> Unit) {
        expense.forEach { execSql("INSERT OR IGNORE INTO categories (name, type) VALUES (?, ?)", arrayOf(it, "EXPENSE")) }
        income.forEach { execSql("INSERT OR IGNORE INTO categories (name, type) VALUES (?, ?)", arrayOf(it, "INCOME")) }
    }
}
