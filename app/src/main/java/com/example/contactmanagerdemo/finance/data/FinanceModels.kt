package com.example.contactmanagerdemo.finance.data

enum class FinanceTransactionType(val dbValue: String) {
    INCOME("income"),
    EXPENSE("expense");

    companion object {
        fun fromDb(value: String): FinanceTransactionType =
            entries.firstOrNull { it.dbValue == value } ?: EXPENSE
    }
}

data class FinanceCategory(
    val id: Long,
    val name: String,
    val type: FinanceTransactionType,
    val color: String,
)

data class FinanceTransaction(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val amountCents: Long,
    val type: FinanceTransactionType,
    val dateIso: String,
    val note: String,
)

data class MonthlySummary(
    val incomeCents: Long,
    val expenseCents: Long,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

data class CategorySpend(
    val categoryId: Long,
    val categoryName: String,
    val color: String,
    val amountCents: Long,
)

data class DailyBalancePoint(
    val dateIso: String,
    val balanceCents: Long,
)

data class BudgetStatus(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val month: String,
    val limitCents: Long,
    val spentCents: Long,
) {
    val ratio: Float
        get() = if (limitCents <= 0L) 0f else spentCents.toFloat() / limitCents.toFloat()

    val percent: Int
        get() = (ratio * 100f).toInt().coerceAtLeast(0)
}
