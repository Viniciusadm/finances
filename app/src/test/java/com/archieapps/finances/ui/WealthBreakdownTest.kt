package com.archieapps.finances.ui

import com.archieapps.finances.data.dto.AccountDto
import com.archieapps.finances.data.dto.CommittedItemDto
import com.archieapps.finances.data.dto.NotFreeDto
import com.archieapps.finances.data.dto.OverviewDto
import com.archieapps.finances.data.dto.SeparatedDto
import com.archieapps.finances.ui.screen.wealth.WealthBreakdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WealthBreakdownTest {

    private fun account(
        id: Int,
        balance: Long,
        kind: String = "checking",
        type: String? = null,
        separated: Boolean = false,
        locked: Boolean = false
    ) = AccountDto(
        id = id,
        name = "Conta $id",
        kind = kind,
        kindLabel = "x",
        investmentType = type,
        separated = separated,
        locked = locked,
        initialBalanceCents = 0,
        balanceCents = balance
    )

    @Test
    fun listsOnlyAccountsAndFixedIncomeThatAreTheUsers() {
        val data = OverviewDto(
            month = "2026-11",
            totalCents = 64_000,
            committedCents = 7_000,
            freeCents = 27_000,
            notFree = NotFreeDto(lockedCents = 30_000),
            separated = SeparatedDto(totalCents = 70_000, accountIds = listOf(7)),
            variable = SeparatedDto(totalCents = 56_000, accountIds = listOf(5, 6)),
            accounts = listOf(
                account(1, 10_000),
                account(2, 20_000, kind = "investment", type = "fixed_income"),
                account(3, 34_000, kind = "investment", type = "fixed_income", locked = true),
                account(5, 50_000, kind = "investment", type = "stock"),
                account(6, 6_000, kind = "investment", type = "crypto"),
                account(7, 70_000, kind = "investment", type = "fixed_income", separated = true)
            )
        )

        val breakdown = WealthBreakdown.from(data)

        assertEquals(listOf(1), breakdown.accounts.map { it.id })
        assertEquals(listOf(2, 3), breakdown.fixedIncome.map { it.id })
        assertEquals(listOf(3), breakdown.lockedAccounts.map { it.id })
        assertEquals(64_000L, breakdown.listedCents)
        assertEquals(breakdown.totalCents, breakdown.listedCents)
        assertEquals(70_000L, breakdown.separatedCents)
        assertTrue(breakdown.hasVariable)
    }

    @Test
    fun keepsTheStatementsThatMakeTheCommittedAmount() {
        val data = OverviewDto(
            month = "2026-11",
            committedCents = 5_000,
            committedItems = listOf(
                CommittedItemDto(cardId = 3, cardName = "Inter Virtual", reference = "2026-12", totalCents = 3_000, current = true),
                CommittedItemDto(cardId = 3, cardName = "Inter Virtual", reference = "2026-11", totalCents = 2_000, current = false)
            )
        )

        val breakdown = WealthBreakdown.from(data)

        assertEquals(5_000L, breakdown.committedItems.sumOf { it.totalCents })
        assertEquals(1, breakdown.committedItems.count { it.current })
    }

    @Test
    fun groupsTheWealthByBankAndTheStatementsByCard() {
        val data = OverviewDto(
            month = "2026-11",
            totalCents = 12_000,
            committedCents = 5_000,
            committedItems = listOf(
                CommittedItemDto(cardId = 3, cardName = "Inter Virtual", reference = "2026-12", totalCents = 3_000, current = true),
                CommittedItemDto(cardId = 4, cardName = "Nubank Virtual", reference = "2026-12", totalCents = 2_000, current = true)
            ),
            futureStatements = listOf(
                CommittedItemDto(cardId = 3, cardName = "Inter Virtual", reference = "2027-01", totalCents = 700)
            ),
            accounts = listOf(
                account(1, 10_000).copy(institution = "Nubank"),
                account(2, 2_000, kind = "investment", type = "fixed_income").copy(institution = "Inter")
            )
        )

        val breakdown = WealthBreakdown.from(data)

        assertEquals(listOf("Nubank" to 10_000L, "Inter" to 2_000L), breakdown.byInstitution)
        assertEquals(listOf("Inter Virtual" to 3_000L, "Nubank Virtual" to 2_000L), breakdown.committedByCard)
        assertEquals(700L, breakdown.futureCents)
    }
}
