package com.example.contactmanagerdemo.finance.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FinanceDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY,
                username TEXT NOT NULL UNIQUE,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                name TEXT NOT NULL,
                type TEXT NOT NULL CHECK(type IN ('income','expense')),
                color TEXT NOT NULL,
                UNIQUE(user_id, name, type)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                category_id INTEGER NOT NULL REFERENCES categories(id),
                amount_cents INTEGER NOT NULL CHECK(amount_cents > 0),
                type TEXT NOT NULL CHECK(type IN ('income','expense')),
                tx_date TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                category_id INTEGER NOT NULL REFERENCES categories(id),
                month TEXT NOT NULL,
                limit_cents INTEGER NOT NULL CHECK(limit_cents > 0),
                UNIQUE(user_id, category_id, month)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_tx_user_date ON transactions(user_id, tx_date)")
        db.execSQL("CREATE INDEX idx_tx_category ON transactions(category_id)")
        db.execSQL("INSERT INTO users(id, username) VALUES($LOCAL_USER_ID, 'Локальный пользователь')")
        insertDefaultCategories(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun getCategories(type: FinanceTransactionType? = null): List<FinanceCategory> {
        val args = mutableListOf(LOCAL_USER_ID.toString())
        val where = buildString {
            append("user_id = ?")
            if (type != null) {
                append(" AND type = ?")
                args += type.dbValue
            }
        }
        return readableDatabase.query(
            "categories",
            arrayOf("id", "name", "type", "color"),
            where,
            args.toTypedArray(),
            null,
            null,
            "type DESC, name COLLATE NOCASE ASC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        FinanceCategory(
                            id = cursor.getLong(0),
                            name = cursor.getString(1),
                            type = FinanceTransactionType.fromDb(cursor.getString(2)),
                            color = cursor.getString(3),
                        ),
                    )
                }
            }
        }
    }

    fun insertCategory(name: String, type: FinanceTransactionType, color: String): Long {
        val values = ContentValues().apply {
            put("user_id", LOCAL_USER_ID)
            put("name", name.trim())
            put("type", type.dbValue)
            put("color", color)
        }
        return writableDatabase.insertOrThrow("categories", null, values)
    }

    fun updateCategory(categoryId: Long, name: String, color: String): Boolean {
        val values = ContentValues().apply {
            put("name", name.trim())
            put("color", color)
        }
        return writableDatabase.update(
            "categories",
            values,
            "id = ? AND user_id = ?",
            arrayOf(categoryId.toString(), LOCAL_USER_ID.toString()),
        ) > 0
    }

    fun deleteCategory(categoryId: Long): Boolean {
        val usedCount = readableDatabase.rawQuery(
            """
            SELECT
                (SELECT COUNT(*) FROM transactions WHERE category_id = ?) +
                (SELECT COUNT(*) FROM budgets WHERE category_id = ?)
            """.trimIndent(),
            arrayOf(categoryId.toString(), categoryId.toString()),
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
        if (usedCount > 0L) return false
        return writableDatabase.delete(
            "categories",
            "id = ? AND user_id = ?",
            arrayOf(categoryId.toString(), LOCAL_USER_ID.toString()),
        ) > 0
    }

    fun insertTransaction(
        category: FinanceCategory,
        amountCents: Long,
        dateIso: String,
        note: String,
    ): Long {
        val values = ContentValues().apply {
            put("user_id", LOCAL_USER_ID)
            put("category_id", category.id)
            put("amount_cents", amountCents)
            put("type", category.type.dbValue)
            put("tx_date", dateIso)
            put("note", note.trim())
        }
        return writableDatabase.insertOrThrow("transactions", null, values)
    }

    fun updateTransaction(
        transactionId: Long,
        category: FinanceCategory,
        amountCents: Long,
        dateIso: String,
        note: String,
    ): Boolean {
        val values = ContentValues().apply {
            put("category_id", category.id)
            put("amount_cents", amountCents)
            put("type", category.type.dbValue)
            put("tx_date", dateIso)
            put("note", note.trim())
        }
        return writableDatabase.update(
            "transactions",
            values,
            "id = ? AND user_id = ?",
            arrayOf(transactionId.toString(), LOCAL_USER_ID.toString()),
        ) > 0
    }

    fun deleteTransaction(transactionId: Long): Boolean =
        writableDatabase.delete(
            "transactions",
            "id = ? AND user_id = ?",
            arrayOf(transactionId.toString(), LOCAL_USER_ID.toString()),
        ) > 0

    fun getTransactions(
        month: String,
        type: FinanceTransactionType? = null,
        categoryId: Long? = null,
    ): List<FinanceTransaction> {
        val args = mutableListOf(LOCAL_USER_ID.toString(), month)
        val extra = buildString {
            if (type != null) {
                append(" AND t.type = ?")
                args += type.dbValue
            }
            if (categoryId != null) {
                append(" AND t.category_id = ?")
                args += categoryId.toString()
            }
        }
        return queryTransactions(
            """
            SELECT t.id, t.category_id, c.name, c.color, t.amount_cents, t.type, t.tx_date, t.note
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            WHERE t.user_id = ? AND substr(t.tx_date, 1, 7) = ?$extra
            ORDER BY t.tx_date DESC, t.id DESC
            """.trimIndent(),
            args.toTypedArray(),
        )
    }

    fun getAllTransactions(): List<FinanceTransaction> = queryTransactions(
        """
        SELECT t.id, t.category_id, c.name, c.color, t.amount_cents, t.type, t.tx_date, t.note
        FROM transactions t
        JOIN categories c ON c.id = t.category_id
        WHERE t.user_id = ?
        ORDER BY t.tx_date DESC, t.id DESC
        """.trimIndent(),
        arrayOf(LOCAL_USER_ID.toString()),
    )

    fun getRecentTransactions(month: String, limit: Int = 3): List<FinanceTransaction> =
        queryTransactions(
            """
            SELECT t.id, t.category_id, c.name, c.color, t.amount_cents, t.type, t.tx_date, t.note
            FROM transactions t
            JOIN categories c ON c.id = t.category_id
            WHERE t.user_id = ? AND substr(t.tx_date, 1, 7) = ?
            ORDER BY t.tx_date DESC, t.id DESC
            LIMIT ?
            """.trimIndent(),
            arrayOf(LOCAL_USER_ID.toString(), month, limit.toString()),
        )

    fun getMonthlySummary(month: String): MonthlySummary = readableDatabase.rawQuery(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'income' THEN amount_cents ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN type = 'expense' THEN amount_cents ELSE 0 END), 0)
        FROM transactions
        WHERE user_id = ? AND substr(tx_date, 1, 7) = ?
        """.trimIndent(),
        arrayOf(LOCAL_USER_ID.toString(), month),
    ).use { cursor ->
        cursor.moveToFirst()
        MonthlySummary(cursor.getLong(0), cursor.getLong(1))
    }

    fun getCategorySpending(month: String): List<CategorySpend> = readableDatabase.rawQuery(
        """
        SELECT c.id, c.name, c.color, SUM(t.amount_cents) AS total
        FROM transactions t
        JOIN categories c ON c.id = t.category_id
        WHERE t.user_id = ? AND t.type = 'expense' AND substr(t.tx_date, 1, 7) = ?
        GROUP BY c.id, c.name, c.color
        ORDER BY total DESC
        """.trimIndent(),
        arrayOf(LOCAL_USER_ID.toString(), month),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(CategorySpend(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3)))
            }
        }
    }

    fun getDailyBalance(month: String): List<DailyBalancePoint> {
        var running = 0L
        return readableDatabase.rawQuery(
            """
            SELECT tx_date,
                SUM(CASE WHEN type = 'income' THEN amount_cents ELSE -amount_cents END) AS delta
            FROM transactions
            WHERE user_id = ? AND substr(tx_date, 1, 7) = ?
            GROUP BY tx_date
            ORDER BY tx_date ASC
            """.trimIndent(),
            arrayOf(LOCAL_USER_ID.toString(), month),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    running += cursor.getLong(1)
                    add(DailyBalancePoint(cursor.getString(0), running))
                }
            }
        }
    }

    fun getBudgetStatuses(month: String): List<BudgetStatus> = readableDatabase.rawQuery(
        """
        SELECT b.id, c.id, c.name, c.color, b.month, b.limit_cents,
            COALESCE(SUM(t.amount_cents), 0) AS spent
        FROM budgets b
        JOIN categories c ON c.id = b.category_id
        LEFT JOIN transactions t ON t.category_id = b.category_id
            AND t.user_id = b.user_id
            AND t.type = 'expense'
            AND substr(t.tx_date, 1, 7) = b.month
        WHERE b.user_id = ? AND b.month = ?
        GROUP BY b.id, c.id, c.name, c.color, b.month, b.limit_cents
        ORDER BY (spent * 1.0 / b.limit_cents) DESC, c.name COLLATE NOCASE ASC
        """.trimIndent(),
        arrayOf(LOCAL_USER_ID.toString(), month),
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    BudgetStatus(
                        id = cursor.getLong(0),
                        categoryId = cursor.getLong(1),
                        categoryName = cursor.getString(2),
                        categoryColor = cursor.getString(3),
                        month = cursor.getString(4),
                        limitCents = cursor.getLong(5),
                        spentCents = cursor.getLong(6),
                    ),
                )
            }
        }
    }

    fun upsertBudget(categoryId: Long, month: String, limitCents: Long): Long {
        val values = ContentValues().apply {
            put("user_id", LOCAL_USER_ID)
            put("category_id", categoryId)
            put("month", month)
            put("limit_cents", limitCents)
        }
        val updated = writableDatabase.update(
            "budgets",
            values,
            "user_id = ? AND category_id = ? AND month = ?",
            arrayOf(LOCAL_USER_ID.toString(), categoryId.toString(), month),
        )
        if (updated > 0) {
            return readableDatabase.rawQuery(
                "SELECT id FROM budgets WHERE user_id = ? AND category_id = ? AND month = ?",
                arrayOf(LOCAL_USER_ID.toString(), categoryId.toString(), month),
            ).use { cursor ->
                cursor.moveToFirst()
                cursor.getLong(0)
            }
        }
        return writableDatabase.insertOrThrow("budgets", null, values)
    }

    fun deleteBudget(budgetId: Long): Boolean = writableDatabase.delete(
        "budgets",
        "id = ? AND user_id = ?",
        arrayOf(budgetId.toString(), LOCAL_USER_ID.toString()),
    ) > 0

    private fun queryTransactions(sql: String, args: Array<String>): List<FinanceTransaction> =
        readableDatabase.rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toTransaction())
            }
        }

    private fun Cursor.toTransaction(): FinanceTransaction = FinanceTransaction(
        id = getLong(0),
        categoryId = getLong(1),
        categoryName = getString(2),
        categoryColor = getString(3),
        amountCents = getLong(4),
        type = FinanceTransactionType.fromDb(getString(5)),
        dateIso = getString(6),
        note = getString(7).orEmpty(),
    )

    private fun insertDefaultCategories(db: SQLiteDatabase) {
        DEFAULT_CATEGORIES.forEach { (name, type, color) ->
            val values = ContentValues().apply {
                put("user_id", LOCAL_USER_ID)
                put("name", name)
                put("type", type.dbValue)
                put("color", color)
            }
            db.insert("categories", null, values)
        }
    }

    companion object {
        const val LOCAL_USER_ID = 1L
        private const val DATABASE_NAME = "finance_manager.db"
        private const val DATABASE_VERSION = 1

        private val DEFAULT_CATEGORIES = listOf(
            Triple("Зарплата", FinanceTransactionType.INCOME, "#0B7A4B"),
            Triple("Подработка", FinanceTransactionType.INCOME, "#2E7D9A"),
            Triple("Продукты", FinanceTransactionType.EXPENSE, "#E05D52"),
            Triple("Транспорт", FinanceTransactionType.EXPENSE, "#E39B2E"),
            Triple("Жильё", FinanceTransactionType.EXPENSE, "#6C63A8"),
            Triple("Здоровье", FinanceTransactionType.EXPENSE, "#2F8F83"),
            Triple("Развлечения", FinanceTransactionType.EXPENSE, "#B65A8B"),
            Triple("Прочее", FinanceTransactionType.EXPENSE, "#6F7782"),
        )
    }
}
