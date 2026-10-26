package com.ashrafwebdev.sikkanam.ui

import com.ashrafwebdev.sikkanam.data.Transaction

/** Builds a CSV file of all transactions, with a header row. Values are quoted when needed. */
fun transactionsToCsv(transactions: List<Transaction>): String {
    val header = "date,type,category,from_or_to,for_what,by_which,amount,halal_status,interest"
    val rows = transactions.sortedBy { it.dateMillis }.map { t ->
        listOf(
            dateLabel(t.dateMillis),
            t.type.name,
            t.category,
            t.party,
            t.note,
            t.paymentMethod,
            formatPlain(t.amountCents),
            t.halalStatus.name,
            if (t.isRiba) "yes" else "no",
        ).joinToString(",") { csvField(it) }
    }
    return (listOf(header) + rows).joinToString("\n", postfix = "\n")
}

private fun csvField(value: String): String {
    val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' }
    return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
