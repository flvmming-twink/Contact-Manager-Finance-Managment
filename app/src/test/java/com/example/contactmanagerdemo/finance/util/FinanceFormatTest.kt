package com.example.contactmanagerdemo.finance.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinanceFormatTest {
    @Test
    fun `parses rubles with comma or dot into exact kopecks`() {
        assertEquals(123_456L, FinanceFormat.parseMoneyToCents("1 234,56"))
        assertEquals(12_350L, FinanceFormat.parseMoneyToCents("123.50"))
        assertEquals(100L, FinanceFormat.parseMoneyToCents("1"))
    }

    @Test
    fun `rejects non positive and over precise amounts`() {
        assertNull(FinanceFormat.parseMoneyToCents("0"))
        assertNull(FinanceFormat.parseMoneyToCents("-10"))
        assertNull(FinanceFormat.parseMoneyToCents("10,001"))
        assertNull(FinanceFormat.parseMoneyToCents("не число"))
    }
}
