package com.archieapps.finances.ui

import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.ui.screen.accounts.patrimonyCents
import com.archieapps.finances.ui.screen.accounts.separatedCents
import com.archieapps.finances.ui.screen.accounts.variableCents
import com.archieapps.finances.ui.screen.holdings.formatPercent
import com.archieapps.finances.ui.screen.holdings.formatPrice
import com.archieapps.finances.ui.screen.holdings.formatQuantity
import com.archieapps.finances.ui.screen.holdings.formatSigned
import com.archieapps.finances.ui.screen.holdings.isPositive
import com.archieapps.finances.ui.screen.holdings.parseDecimal
import com.archieapps.finances.ui.screen.holdings.valueCents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class HoldingNumbersTest {

    @Test
    fun parsesBrazilianAndPlainNumbers() {
        assertEquals(BigDecimal("35.20"), parseDecimal("35,20"))
        assertEquals(BigDecimal("1234.56"), parseDecimal("1.234,56"))
        assertEquals(BigDecimal("0.00012"), parseDecimal("0.00012"))
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("abc"))
    }

    @Test
    fun positiveOnlyForNumbersAboveZero() {
        assertTrue(isPositive("0,5"))
        assertFalse(isPositive("0"))
        assertFalse(isPositive(""))
    }

    @Test
    fun valueIsQuantityTimesPriceInCents() {
        assertEquals(35200L, valueCents("10", "35,20"))
        assertEquals(1L, valueCents("0,04", "0,30"))
        assertEquals(2L, valueCents("0,05", "0,30"))
        assertNull(valueCents("", "10"))
    }

    @Test
    fun formatsQuantityAndPrice() {
        assertEquals("10", formatQuantity("10.00000000"))
        assertEquals("0,5", formatQuantity("0.50000000"))
        assertEquals("35,20", formatPrice("35.20000000"))
        assertEquals("1.234,50", formatPrice("1234.5"))
        assertEquals("0,00012", formatPrice("0.00012000"))
    }

    @Test
    fun formatsProfitWithSign() {
        assertEquals("+R$ 22,00", formatSigned(2200))
        assertEquals("−R$ 5,50", formatSigned(-550))
        assertEquals("R$ 0,00", formatSigned(0))
        assertEquals("+6,67%", formatPercent(6.67))
        assertEquals("-3,50%", formatPercent(-3.5))
        assertEquals("—", formatPercent(null))
    }

    private fun account(
        id: Int,
        balance: Long,
        separated: Boolean = false,
        archived: Boolean = false,
        kind: String = "checking",
        type: String? = null
    ) = AccountDto(
        id = id,
        name = "Conta $id",
        kind = kind,
        kindLabel = "Conta corrente",
        investmentType = type,
        separated = separated,
        initialBalanceCents = 0,
        balanceCents = balance,
        archived = archived
    )

    @Test
    fun patrimonyIgnoresSeparatedAndArchivedAccounts() {
        val accounts = listOf(
            account(1, 23_878),
            account(2, 244_000),
            account(3, 107_258, separated = true),
            account(4, 5_000, archived = true)
        )

        assertEquals(267_878L, patrimonyCents(accounts))
        assertEquals(107_258L, separatedCents(accounts))
    }

    @Test
    fun patrimonyLeavesOutStocksAndCryptoButKeepsFixedIncome() {
        val accounts = listOf(
            account(1, 10_000),
            account(2, 20_000, kind = "investment", type = "fixed_income"),
            account(3, 50_000, kind = "investment", type = "stock"),
            account(4, 6_000, kind = "investment", type = "crypto")
        )

        assertEquals(30_000L, patrimonyCents(accounts))
        assertEquals(56_000L, variableCents(accounts))
    }
}
