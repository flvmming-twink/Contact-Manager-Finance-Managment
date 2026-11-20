package com.example.contactmanagerdemo.finance.ui

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanagerdemo.R
import com.example.contactmanagerdemo.finance.data.BudgetStatus
import com.example.contactmanagerdemo.finance.data.CategorySpend
import com.example.contactmanagerdemo.finance.data.FinanceCategory
import com.example.contactmanagerdemo.finance.data.FinanceDatabase
import com.example.contactmanagerdemo.finance.data.FinanceTransaction
import com.example.contactmanagerdemo.finance.data.FinanceTransactionType
import com.example.contactmanagerdemo.finance.util.FinanceFormat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FinanceActivity : AppCompatActivity() {

    private lateinit var database: FinanceDatabase
    private lateinit var root: View
    private lateinit var toolbar: MaterialToolbar
    private lateinit var content: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fab: FloatingActionButton

    private var currentScreen = Screen.OVERVIEW
    private var selectedMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
    private var transactionFilterType: FinanceTransactionType? = null
    private var transactionFilterCategoryId: Long? = null
    private val exportCsvLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@registerForActivityResult
        val success = runCatching {
            contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write("\uFEFF")
                    writer.write(buildCsv())
                }
            } ?: error("Cannot open output stream")
        }.isSuccess
        Snackbar.make(root, if (success) R.string.finance_csv_saved else R.string.finance_csv_error, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finance)
        database = FinanceDatabase(this)
        root = findViewById(R.id.financeRoot)
        toolbar = findViewById(R.id.financeToolbar)
        content = findViewById(R.id.financeContent)
        bottomNavigation = findViewById(R.id.financeBottomNavigation)
        fab = findViewById(R.id.financeFab)

        toolbar.subtitle = getString(R.string.finance_version)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_export_csv) {
                startCsvExport()
                true
            } else false
        }
        bottomNavigation.setOnItemSelectedListener { item ->
            currentScreen = when (item.itemId) {
                R.id.nav_transactions -> Screen.TRANSACTIONS
                R.id.nav_budgets -> Screen.BUDGETS
                R.id.nav_categories -> Screen.CATEGORIES
                else -> Screen.OVERVIEW
            }
            renderScreen()
            true
        }
        bottomNavigation.setOnItemReselectedListener { renderScreen() }
        fab.setOnClickListener {
            when (currentScreen) {
                Screen.BUDGETS -> showBudgetDialog(null)
                Screen.OVERVIEW, Screen.TRANSACTIONS -> showTransactionDialog(null)
                Screen.CATEGORIES -> showCategoryDialog(null)
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_overview
    }

    override fun onDestroy() {
        database.close()
        super.onDestroy()
    }

    private fun renderScreen() {
        content.removeAllViews()
        val layout = when (currentScreen) {
            Screen.OVERVIEW -> R.layout.screen_finance_overview
            Screen.TRANSACTIONS -> R.layout.screen_finance_transactions
            Screen.BUDGETS -> R.layout.screen_finance_budgets
            Screen.CATEGORIES -> R.layout.screen_finance_categories
        }
        val view = layoutInflater.inflate(layout, content, false)
        content.addView(view)
        toolbar.title = getString(when (currentScreen) {
            Screen.OVERVIEW -> R.string.finance_title_overview
            Screen.TRANSACTIONS -> R.string.finance_title_transactions
            Screen.BUDGETS -> R.string.finance_title_budgets
            Screen.CATEGORIES -> R.string.finance_title_categories
        })
        fab.isVisible = currentScreen != Screen.CATEGORIES
        fab.contentDescription = getString(if (currentScreen == Screen.BUDGETS) R.string.finance_add_budget else R.string.finance_add_transaction)

        when (currentScreen) {
            Screen.OVERVIEW -> setupOverview(view)
            Screen.TRANSACTIONS -> setupTransactions(view)
            Screen.BUDGETS -> setupBudgets(view)
            Screen.CATEGORIES -> setupCategories(view)
        }
    }

    private fun setupMonthSelector(view: View) {
        view.findViewById<TextView>(R.id.textMonth).text = FinanceFormat.monthLabel(selectedMonth)
        view.findViewById<View>(R.id.btnPreviousMonth).setOnClickListener { shiftMonth(-1) }
        view.findViewById<View>(R.id.btnNextMonth).setOnClickListener { shiftMonth(1) }
    }

    private fun shiftMonth(delta: Int) {
        val format = SimpleDateFormat("yyyy-MM", Locale.US).apply { isLenient = false }
        val calendar = Calendar.getInstance().apply {
            time = format.parse(selectedMonth) ?: Calendar.getInstance().time
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, delta)
        }
        selectedMonth = format.format(calendar.time)
        renderScreen()
    }

    private fun setupOverview(view: View) {
        setupMonthSelector(view)
        val summary = database.getMonthlySummary(selectedMonth)
        view.findViewById<TextView>(R.id.textBalance).text = FinanceFormat.money(summary.balanceCents, signed = true)
        view.findViewById<TextView>(R.id.textIncome).text = FinanceFormat.money(summary.incomeCents)
        view.findViewById<TextView>(R.id.textExpense).text = FinanceFormat.money(summary.expenseCents)

        val urgent = database.getBudgetStatuses(selectedMonth).firstOrNull { it.ratio >= 0.8f }
        val warningCard = view.findViewById<MaterialCardView>(R.id.cardBudgetWarning)
        warningCard.isVisible = urgent != null
        if (urgent != null) {
            view.findViewById<TextView>(R.id.textBudgetWarning).text =
                getString(R.string.finance_budget_warning, urgent.categoryName, urgent.percent)
            warningCard.setOnClickListener { bottomNavigation.selectedItemId = R.id.nav_budgets }
        }

        val spending = database.getCategorySpending(selectedMonth)
        view.findViewById<CategoryDonutView>(R.id.donutChart).setData(spending)
        populateCategoryLegend(view.findViewById(R.id.categoryLegend), spending)
        view.findViewById<BalanceTrendView>(R.id.balanceTrend).setData(database.getDailyBalance(selectedMonth))

        val recent = database.getRecentTransactions(selectedMonth)
        val recentContainer = view.findViewById<LinearLayout>(R.id.recentTransactions)
        recent.forEach { item ->
            val row = layoutInflater.inflate(R.layout.item_finance_transaction, recentContainer, false)
            bindTransactionRow(row, item, compact = true)
            recentContainer.addView(row)
        }
        view.findViewById<View>(R.id.textOverviewEmpty).isVisible = recent.isEmpty()
    }

    private fun populateCategoryLegend(container: LinearLayout, spending: List<CategorySpend>) {
        container.removeAllViews()
        if (spending.isEmpty()) {
            container.addView(TextView(this).apply {
                text = getString(R.string.finance_no_transactions)
                setTextColor(ContextCompat.getColor(context, R.color.finance_on_surface_variant))
                textSize = 14f
            })
            return
        }
        spending.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, dp(5))
            }
            row.addView(View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(runCatching { Color.parseColor(item.color) }.getOrDefault(Color.GRAY))
                }
            }, LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(8) })
            row.addView(TextView(this).apply {
                text = item.categoryName
                setTextColor(ContextCompat.getColor(context, R.color.finance_on_surface))
                textSize = 13f
                maxLines = 1
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(TextView(this).apply {
                text = FinanceFormat.money(item.amountCents)
                setTextColor(ContextCompat.getColor(context, R.color.finance_on_surface_variant))
                textSize = 13f
                setPadding(dp(6), 0, 0, 0)
            })
            container.addView(row)
        }
    }

    private fun setupTransactions(view: View) {
        setupMonthSelector(view)
        val recycler = view.findViewById<RecyclerView>(R.id.transactionsList)
        val empty = view.findViewById<View>(R.id.transactionsEmpty)
        val adapter = FinanceTransactionAdapter(
            onOpen = { showTransactionDialog(it) },
            onMore = { anchor, item -> showTransactionMenu(anchor, item) },
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        val categories = database.getCategories()
        if (categories.none { it.id == transactionFilterCategoryId }) transactionFilterCategoryId = null
        val categorySpinner = view.findViewById<Spinner>(R.id.categoryFilter)
        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.finance_filter_category_all)) + categories.map { it.name },
        )
        categorySpinner.setSelection(transactionFilterCategoryId?.let { id ->
            categories.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.plus(1)
        } ?: 0)

        fun reload() {
            val items = database.getTransactions(selectedMonth, transactionFilterType, transactionFilterCategoryId)
            adapter.submitList(items)
            recycler.isVisible = items.isNotEmpty()
            empty.isVisible = items.isEmpty()
        }
        view.findViewById<MaterialButtonToggleGroup>(R.id.typeFilterGroup).apply {
            check(when (transactionFilterType) {
                FinanceTransactionType.INCOME -> R.id.filterIncome
                FinanceTransactionType.EXPENSE -> R.id.filterExpense
                null -> R.id.filterAll
            })
            addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                transactionFilterType = when (checkedId) {
                    R.id.filterIncome -> FinanceTransactionType.INCOME
                    R.id.filterExpense -> FinanceTransactionType.EXPENSE
                    else -> null
                }
                reload()
            }
        }
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) {
                transactionFilterCategoryId = if (position == 0) null else categories[position - 1].id
                reload()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        reload()
    }

    private fun setupBudgets(view: View) {
        setupMonthSelector(view)
        val recycler = view.findViewById<RecyclerView>(R.id.budgetsList)
        val empty = view.findViewById<View>(R.id.budgetsEmpty)
        val adapter = FinanceBudgetAdapter(
            onOpen = { showBudgetDialog(it) },
            onMore = { anchor, item -> showBudgetMenu(anchor, item) },
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        val items = database.getBudgetStatuses(selectedMonth)
        adapter.submitList(items)
        recycler.isVisible = items.isNotEmpty()
        empty.isVisible = items.isEmpty()
    }

    private fun setupCategories(view: View) {
        val recycler = view.findViewById<RecyclerView>(R.id.categoriesList)
        val adapter = FinanceCategoryAdapter(
            onOpen = { showCategoryDialog(it) },
            onMore = { anchor, item -> showCategoryMenu(anchor, item) },
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        adapter.submitList(database.getCategories())
        view.findViewById<View>(R.id.addCategoryButton).setOnClickListener { showCategoryDialog(null) }
    }

    private fun showTransactionDialog(existing: FinanceTransaction?) {
        val categories = database.getCategories()
        if (categories.isEmpty()) {
            Snackbar.make(root, R.string.finance_add_category, Snackbar.LENGTH_LONG).show()
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_finance_transaction, null)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.dialogTransactionCategory)
        val amountLayout = dialogView.findViewById<TextInputLayout>(R.id.dialogTransactionAmountLayout)
        val amountInput = dialogView.findViewById<EditText>(R.id.dialogTransactionAmount)
        val dateInput = dialogView.findViewById<EditText>(R.id.dialogTransactionDate)
        val noteInput = dialogView.findViewById<EditText>(R.id.dialogTransactionNote)
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories.map { categoryLabel(it) })
        existing?.let { item ->
            val index = categories.indexOfFirst { it.id == item.categoryId }
            if (index >= 0) categorySpinner.setSelection(index)
            amountInput.setText(centsToInput(item.amountCents))
            dateInput.setText(item.dateIso)
            noteInput.setText(item.note)
        } ?: dateInput.setText(FinanceFormat.todayIso())
        dateInput.setOnClickListener { showDatePicker(dateInput) }
        dateInput.setOnKeyListener { _, keyCode, event ->
            val opensPicker = keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_SPACE
            if (opensPicker && event.action == KeyEvent.ACTION_UP) {
                showDatePicker(dateInput)
                true
            } else false
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) "Новая операция" else "Изменить операцию")
            .setView(dialogView)
            .setNegativeButton(R.string.finance_cancel, null)
            .setPositiveButton(R.string.finance_save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                amountLayout.error = null
                val amountCents = FinanceFormat.parseMoneyToCents(amountInput.text.toString())
                if (amountCents == null) {
                    amountLayout.error = getString(R.string.finance_invalid_amount)
                    return@setOnClickListener
                }
                val date = dateInput.text.toString()
                if (date > FinanceFormat.todayIso()) {
                    Snackbar.make(dialogView, R.string.finance_invalid_date, Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val category = categories[categorySpinner.selectedItemPosition]
                runCatching {
                    if (existing == null) database.insertTransaction(category, amountCents, date, noteInput.text.toString())
                    else database.updateTransaction(existing.id, category, amountCents, date, noteInput.text.toString())
                }.onSuccess {
                    dialog.dismiss()
                    selectedMonth = date.take(7)
                    renderScreen()
                    showBudgetCrossingIfNeeded(category.id, selectedMonth)
                }.onFailure {
                    Snackbar.make(dialogView, it.message ?: getString(R.string.finance_invalid_amount), Snackbar.LENGTH_LONG).show()
                }
            }
        }
        dialog.show()
    }

    private fun showDatePicker(input: EditText) {
        val parts = input.text.toString().split('-').mapNotNull { it.toIntOrNull() }
        val now = Calendar.getInstance()
        val year = parts.getOrNull(0) ?: now.get(Calendar.YEAR)
        val month = (parts.getOrNull(1) ?: now.get(Calendar.MONTH) + 1) - 1
        val day = parts.getOrNull(2) ?: now.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(this, { _, y, m, d ->
            input.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
        }, year, month, day).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
    }

    private fun showBudgetDialog(existing: BudgetStatus?) {
        val categories = database.getCategories(FinanceTransactionType.EXPENSE)
        if (categories.isEmpty()) {
            Snackbar.make(root, R.string.finance_add_category, Snackbar.LENGTH_LONG).show()
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_finance_budget, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.dialogBudgetCategory)
        val amountLayout = dialogView.findViewById<TextInputLayout>(R.id.dialogBudgetAmountLayout)
        val amount = dialogView.findViewById<EditText>(R.id.dialogBudgetAmount)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories.map { it.name })
        existing?.let { budget ->
            val index = categories.indexOfFirst { it.id == budget.categoryId }
            if (index >= 0) spinner.setSelection(index)
            amount.setText(centsToInput(budget.limitCents))
            spinner.isEnabled = false
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) "Новый бюджет · ${FinanceFormat.monthLabel(selectedMonth)}" else "Изменить бюджет")
            .setView(dialogView)
            .setNegativeButton(R.string.finance_cancel, null)
            .setPositiveButton(R.string.finance_save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                amountLayout.error = null
                val limit = FinanceFormat.parseMoneyToCents(amount.text.toString())
                if (limit == null) {
                    amountLayout.error = getString(R.string.finance_invalid_amount)
                    return@setOnClickListener
                }
                database.upsertBudget(categories[spinner.selectedItemPosition].id, selectedMonth, limit)
                dialog.dismiss()
                renderScreen()
                Snackbar.make(root, R.string.finance_saved, Snackbar.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun showCategoryDialog(existing: FinanceCategory?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_finance_category, null)
        val nameLayout = dialogView.findViewById<TextInputLayout>(R.id.dialogCategoryNameLayout)
        val name = dialogView.findViewById<EditText>(R.id.dialogCategoryName)
        val typeGroup = dialogView.findViewById<MaterialButtonToggleGroup>(R.id.dialogCategoryTypeGroup)
        val colorSpinner = dialogView.findViewById<Spinner>(R.id.dialogCategoryColor)
        val colors = categoryColors()
        colorSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colors.map { it.first })
        existing?.let { category ->
            name.setText(category.name)
            typeGroup.check(if (category.type == FinanceTransactionType.INCOME) R.id.dialogTypeIncome else R.id.dialogTypeExpense)
            dialogView.findViewById<View>(R.id.dialogTypeIncome).isEnabled = false
            dialogView.findViewById<View>(R.id.dialogTypeExpense).isEnabled = false
            val index = colors.indexOfFirst { it.second.equals(category.color, ignoreCase = true) }
            if (index >= 0) colorSpinner.setSelection(index)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) "Новая категория" else "Изменить категорию")
            .setView(dialogView)
            .setNegativeButton(R.string.finance_cancel, null)
            .setPositiveButton(R.string.finance_save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                nameLayout.error = null
                val cleanName = name.text.toString().trim()
                if (cleanName.isBlank()) {
                    nameLayout.error = getString(R.string.finance_invalid_name)
                    return@setOnClickListener
                }
                val type = if (typeGroup.checkedButtonId == R.id.dialogTypeIncome) FinanceTransactionType.INCOME else FinanceTransactionType.EXPENSE
                val color = colors[colorSpinner.selectedItemPosition].second
                runCatching {
                    if (existing == null) database.insertCategory(cleanName, type, color)
                    else database.updateCategory(existing.id, cleanName, color)
                }.onSuccess {
                    dialog.dismiss()
                    renderScreen()
                    Snackbar.make(root, R.string.finance_saved, Snackbar.LENGTH_SHORT).show()
                }.onFailure { nameLayout.error = getString(R.string.finance_duplicate_category) }
            }
        }
        dialog.show()
    }

    private fun showTransactionMenu(anchor: View, item: FinanceTransaction) {
        PopupMenu(this, anchor).apply {
            menu.add(0, MENU_EDIT, 0, R.string.finance_edit)
            menu.add(0, MENU_DELETE, 1, R.string.finance_delete)
            setOnMenuItemClickListener { menuItem ->
                if (menuItem.itemId == MENU_EDIT) showTransactionDialog(item) else confirmDeleteTransaction(item)
                true
            }
        }.show()
    }

    private fun showBudgetMenu(anchor: View, item: BudgetStatus) {
        PopupMenu(this, anchor).apply {
            menu.add(0, MENU_EDIT, 0, R.string.finance_edit)
            menu.add(0, MENU_DELETE, 1, R.string.finance_delete)
            setOnMenuItemClickListener { menuItem ->
                if (menuItem.itemId == MENU_EDIT) showBudgetDialog(item) else confirmDeleteBudget(item)
                true
            }
        }.show()
    }

    private fun showCategoryMenu(anchor: View, item: FinanceCategory) {
        PopupMenu(this, anchor).apply {
            menu.add(0, MENU_EDIT, 0, R.string.finance_edit)
            menu.add(0, MENU_DELETE, 1, R.string.finance_delete)
            setOnMenuItemClickListener { menuItem ->
                if (menuItem.itemId == MENU_EDIT) showCategoryDialog(item) else confirmDeleteCategory(item)
                true
            }
        }.show()
    }

    private fun confirmDeleteTransaction(item: FinanceTransaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.finance_delete_transaction_title)
            .setMessage(R.string.finance_delete_transaction_message)
            .setNegativeButton(R.string.finance_cancel, null)
            .setPositiveButton(R.string.finance_delete) { _, _ ->
                database.deleteTransaction(item.id)
                renderScreen()
                Snackbar.make(root, R.string.finance_deleted, Snackbar.LENGTH_SHORT).show()
            }.show()
    }

    private fun confirmDeleteBudget(item: BudgetStatus) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.finance_delete_budget_title)
            .setNegativeButton(R.string.finance_cancel, null)
            .setPositiveButton(R.string.finance_delete) { _, _ ->
                database.deleteBudget(item.id)
                renderScreen()
            }.show()
    }

    private fun confirmDeleteCategory(item: FinanceCategory) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.finance_delete_category_title)
            .setMessage(item.name)
            .setNegativeButton(R.string.finance_cancel, null)
            .setPositiveButton(R.string.finance_delete) { _, _ ->
                if (database.deleteCategory(item.id)) {
                    renderScreen()
                    Snackbar.make(root, R.string.finance_deleted, Snackbar.LENGTH_SHORT).show()
                } else Snackbar.make(root, R.string.finance_delete_category_used, Snackbar.LENGTH_LONG).show()
            }.show()
    }

    private fun bindTransactionRow(row: View, item: FinanceTransaction, compact: Boolean) {
        val dot = row.findViewById<View>(R.id.categoryDot)
        dot.background = dot.background.mutate().apply {
            setTint(runCatching { Color.parseColor(item.categoryColor) }.getOrDefault(Color.GRAY))
        }
        row.findViewById<TextView>(R.id.transactionCategory).text = item.categoryName
        row.findViewById<TextView>(R.id.transactionDetails).text = listOf(FinanceFormat.dateLabel(item.dateIso), item.note)
            .filter { it.isNotBlank() }.joinToString(" · ")
        val amount = row.findViewById<TextView>(R.id.transactionAmount)
        val signed = if (item.type == FinanceTransactionType.INCOME) item.amountCents else -item.amountCents
        amount.text = FinanceFormat.money(signed, signed = true)
        amount.setTextColor(ContextCompat.getColor(this, if (item.type == FinanceTransactionType.INCOME) R.color.finance_primary else R.color.finance_expense))
        row.findViewById<ImageButton>(R.id.transactionMore).isVisible = !compact
        row.setOnClickListener { showTransactionDialog(item) }
    }

    private fun showBudgetCrossingIfNeeded(categoryId: Long, month: String) {
        val budget = database.getBudgetStatuses(month).firstOrNull { it.categoryId == categoryId && it.ratio >= 1f } ?: return
        Snackbar.make(root, getString(R.string.finance_budget_warning, budget.categoryName, budget.percent), Snackbar.LENGTH_LONG)
            .setAction(R.string.finance_title_budgets) { bottomNavigation.selectedItemId = R.id.nav_budgets }
            .show()
    }

    private fun startCsvExport() {
        exportCsvLauncher.launch("FinanceManager-v1.01-transactions.csv")
    }

    private fun buildCsv(): String {
        val builder = StringBuilder("Дата;Тип;Категория;Сумма;Комментарий\r\n")
        database.getAllTransactions().forEach { row ->
            val type = if (row.type == FinanceTransactionType.INCOME) "Доход" else "Расход"
            builder.append(csvCell(row.dateIso)).append(';')
                .append(csvCell(type)).append(';')
                .append(csvCell(row.categoryName)).append(';')
                .append(csvCell(centsToInput(row.amountCents))).append(';')
                .append(csvCell(row.note)).append("\r\n")
        }
        return builder.toString()
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
    private fun categoryLabel(category: FinanceCategory): String = "${if (category.type == FinanceTransactionType.INCOME) "+" else "−"} ${category.name}"
    private fun categoryColors(): List<Pair<String, String>> = listOf(
        getString(R.string.finance_color_emerald) to "#0B7A4B",
        getString(R.string.finance_color_coral) to "#E05D52",
        getString(R.string.finance_color_amber) to "#E39B2E",
        getString(R.string.finance_color_violet) to "#6C63A8",
        getString(R.string.finance_color_teal) to "#2F8F83",
        getString(R.string.finance_color_slate) to "#6F7782",
    )
    private fun centsToInput(cents: Long): String = BigDecimal(cents).movePointLeft(2).stripTrailingZeros().toPlainString()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class Screen { OVERVIEW, TRANSACTIONS, BUDGETS, CATEGORIES }

    companion object {
        private const val MENU_EDIT = 1
        private const val MENU_DELETE = 2
    }
}
