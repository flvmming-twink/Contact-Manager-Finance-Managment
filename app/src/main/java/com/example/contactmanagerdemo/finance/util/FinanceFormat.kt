package com.example.contactmanagerdemo.finance.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FinanceFormat {
    private val ruLocale = Locale("ru", "RU")
    private val moneySymbols = DecimalFormatSymbols(ruLocale).apply {
        groupingSeparator = '\u00A0'
        decimalSeparator = ','
    }
    private val moneyFormat = DecimalFormat("#,##0.##", moneySymbols)
    private val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    private val humanDate = SimpleDateFormat("d MMM", ruLocale)
    private val monthDate = SimpleDateFormat("LLLL yyyy", ruLocale)

    fun parseMoneyToCents(raw: String): Long? = runCatching {
        val normalized = raw.trim().replace(" ", "").replace('\u00A0'.toString(), "").replace(',', '.')
        if (normalized.isBlank()) return null
        val value = BigDecimal(normalized).setScale(2, RoundingMode.UNNECESSARY)
        if (value <= BigDecimal.ZERO) return null
        value.movePointRight(2).longValueExact()
    }.getOrNull()

    @Synchronized
    fun money(cents: Long, signed: Boolean = false): String {
        val value = BigDecimal(cents).movePointLeft(2)
        val prefix = if (signed && cents > 0) "+" else ""
        return "$prefix${moneyFormat.format(value)} ₽"
    }

    @Synchronized
    fun dateLabel(iso: String): String = runCatching {
        val date = isoDate.parse(iso) ?: return iso
        humanDate.format(date)
    }.getOrDefault(iso)

    @Synchronized
    fun monthLabel(month: String): String = runCatching {
        val date = SimpleDateFormat("yyyy-MM", Locale.US).apply { isLenient = false }.parse(month)
            ?: return month
        monthDate.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString() }
    }.getOrDefault(month)

    @Synchronized
    fun todayIso(): String = isoDate.format(Date())
}
