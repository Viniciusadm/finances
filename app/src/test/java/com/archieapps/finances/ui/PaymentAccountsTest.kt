package com.archieapps.finances.ui

import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.ui.components.paymentAccounts
import com.archieapps.finances.ui.screen.composer.ComposerState
import com.archieapps.finances.ui.screen.composer.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentAccountsTest {

    private fun account(id: Int, kind: String) = AccountDto(
        id = id,
        name = "Conta $id",
        kind = kind,
        kindLabel = kind,
        initialBalanceCents = 0
    )

    private val accounts = listOf(
        account(1, "checking"),
        account(2, "savings"),
        account(3, "investment"),
        account(4, "cash"),
        account(5, "wallet"),
        account(6, "checking")
    )

    @Test
    fun onlyCheckingAccountsCanPay() {
        assertEquals(listOf(1, 6), paymentAccounts(accounts).map { it.id })
    }

    @Test
    fun anExpenseNeedsACheckingAccountOrACard() {
        val base = ComposerState(integerDigits = "10", accounts = accounts)

        assertFalse(base.copy(accountId = null).canSave)
        assertFalse(base.copy(accountId = 2).canSave)
        assertFalse(base.copy(accountId = 3).canSave)
        assertTrue(base.copy(accountId = 1).canSave)
        assertFalse(base.copy(target = Target.Card, cardId = null).canSave)
        assertTrue(base.copy(target = Target.Card, cardId = 9).canSave)
    }

    @Test
    fun transfersAndYieldsKeepAnyAccount() {
        val base = ComposerState(integerDigits = "10", accounts = accounts)

        assertTrue(base.copy(kind = "transfer", accountId = 2, counterAccountId = 3).canSave)
        assertTrue(base.copy(kind = "yield", accountId = 3).canSave)
    }
}
