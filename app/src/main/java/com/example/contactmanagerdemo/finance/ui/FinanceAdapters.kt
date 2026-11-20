package com.example.contactmanagerdemo.finance.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contactmanagerdemo.R
import com.example.contactmanagerdemo.finance.data.BudgetStatus
import com.example.contactmanagerdemo.finance.data.FinanceCategory
import com.example.contactmanagerdemo.finance.data.FinanceTransaction
import com.example.contactmanagerdemo.finance.data.FinanceTransactionType
import com.example.contactmanagerdemo.finance.util.FinanceFormat
import com.google.android.material.progressindicator.LinearProgressIndicator

class FinanceTransactionAdapter(
    private val onOpen: (FinanceTransaction) -> Unit,
    private val onMore: (View, FinanceTransaction) -> Unit,
) : ListAdapter<FinanceTransaction, FinanceTransactionAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_finance_transaction, parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dot: View = itemView.findViewById(R.id.categoryDot)
        private val category: TextView = itemView.findViewById(R.id.transactionCategory)
        private val details: TextView = itemView.findViewById(R.id.transactionDetails)
        private val amount: TextView = itemView.findViewById(R.id.transactionAmount)
        private val more: ImageButton = itemView.findViewById(R.id.transactionMore)

        fun bind(item: FinanceTransaction) {
            dot.background = dot.background.mutate().apply {
                setTint(runCatching { Color.parseColor(item.categoryColor) }.getOrDefault(Color.GRAY))
            }
            category.text = item.categoryName
            details.text = listOf(FinanceFormat.dateLabel(item.dateIso), item.note)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            val signed = if (item.type == FinanceTransactionType.INCOME) item.amountCents else -item.amountCents
            amount.text = FinanceFormat.money(signed, signed = true)
            amount.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (item.type == FinanceTransactionType.INCOME) R.color.finance_primary else R.color.finance_expense,
                ),
            )
            itemView.contentDescription = "${item.categoryName}, ${details.text}, ${amount.text}"
            itemView.setOnClickListener { onOpen(item) }
            more.setOnClickListener { onMore(it, item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FinanceTransaction>() {
            override fun areItemsTheSame(oldItem: FinanceTransaction, newItem: FinanceTransaction) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FinanceTransaction, newItem: FinanceTransaction) = oldItem == newItem
        }
    }
}

class FinanceBudgetAdapter(
    private val onOpen: (BudgetStatus) -> Unit,
    private val onMore: (View, BudgetStatus) -> Unit,
) : ListAdapter<BudgetStatus, FinanceBudgetAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_finance_budget, parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val category: TextView = itemView.findViewById(R.id.budgetCategory)
        private val progress: LinearProgressIndicator = itemView.findViewById(R.id.budgetProgress)
        private val values: TextView = itemView.findViewById(R.id.budgetValues)
        private val state: TextView = itemView.findViewById(R.id.budgetState)
        private val more: ImageButton = itemView.findViewById(R.id.budgetMore)

        fun bind(item: BudgetStatus) {
            category.text = item.categoryName
            progress.progress = item.percent.coerceIn(0, 100)
            values.text = itemView.context.getString(
                R.string.finance_budget_values,
                FinanceFormat.money(item.spentCents),
                FinanceFormat.money(item.limitCents),
            )
            val stateRes: Int
            val colorRes: Int
            when {
                item.ratio >= 1f -> {
                    stateRes = R.string.finance_budget_exceeded
                    colorRes = R.color.finance_expense
                }
                item.ratio >= 0.8f -> {
                    stateRes = R.string.finance_budget_near
                    colorRes = R.color.finance_warning
                }
                else -> {
                    stateRes = R.string.finance_budget_normal
                    colorRes = R.color.finance_primary
                }
            }
            val color = ContextCompat.getColor(itemView.context, colorRes)
            state.setText(stateRes)
            state.setTextColor(color)
            progress.setIndicatorColor(color)
            itemView.contentDescription = "${item.categoryName}, ${values.text}, ${state.text}"
            itemView.setOnClickListener { onOpen(item) }
            more.setOnClickListener { onMore(it, item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BudgetStatus>() {
            override fun areItemsTheSame(oldItem: BudgetStatus, newItem: BudgetStatus) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: BudgetStatus, newItem: BudgetStatus) = oldItem == newItem
        }
    }
}

class FinanceCategoryAdapter(
    private val onOpen: (FinanceCategory) -> Unit,
    private val onMore: (View, FinanceCategory) -> Unit,
) : ListAdapter<FinanceCategory, FinanceCategoryAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_finance_category, parent, false),
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val color: View = itemView.findViewById(R.id.categoryColor)
        private val name: TextView = itemView.findViewById(R.id.categoryName)
        private val type: TextView = itemView.findViewById(R.id.categoryType)
        private val more: ImageButton = itemView.findViewById(R.id.categoryMore)

        fun bind(item: FinanceCategory) {
            color.background = color.background.mutate().apply {
                setTint(runCatching { Color.parseColor(item.color) }.getOrDefault(Color.GRAY))
            }
            name.text = item.name
            type.setText(
                if (item.type == FinanceTransactionType.INCOME) {
                    R.string.finance_transaction_income
                } else {
                    R.string.finance_transaction_expense
                },
            )
            itemView.setOnClickListener { onOpen(item) }
            more.setOnClickListener { onMore(it, item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FinanceCategory>() {
            override fun areItemsTheSame(oldItem: FinanceCategory, newItem: FinanceCategory) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: FinanceCategory, newItem: FinanceCategory) = oldItem == newItem
        }
    }
}
