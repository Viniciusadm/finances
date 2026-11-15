package com.archieapps.finances.ui

import com.archieapps.finances.ui.screen.composer.statementFor
import org.junit.Assert.assertEquals
import java.time.LocalDate
import org.junit.Test

class StatementPreviewTest {

    // Inter: fecha dia 1, vence dia 7.
    private fun inter(date: String, shift: Int = 0) = statementFor(1, 7, LocalDate.parse(date), shift)

    @Test
    fun purchaseBeforeClosingFallsInTheNextClosing() {
        val statement = inter("2026-10-30")

        assertEquals(LocalDate.parse("2026-11-01"), statement.closesOn)
        assertEquals(LocalDate.parse("2026-11-07"), statement.dueOn)
        assertEquals("nov/2026 · vence 07/11", statement.label())
    }

    @Test
    fun shiftMovesToTheFollowingStatement() {
        val statement = inter("2026-10-30", shift = 1)

        assertEquals(LocalDate.parse("2026-12-07"), statement.dueOn)
        assertEquals("dez/2026 · vence 07/12", statement.label())
    }

    @Test
    fun closingDayItselfBelongsToThatStatement() {
        assertEquals(LocalDate.parse("2026-11-07"), inter("2026-11-01").dueOn)
        assertEquals(LocalDate.parse("2026-12-07"), inter("2026-11-02").dueOn)
    }

    @Test
    fun yearRolloverAndPreviousShift() {
        assertEquals(LocalDate.parse("2027-01-07"), inter("2026-12-15").dueOn)
        assertEquals(LocalDate.parse("2027-02-07"), inter("2026-12-15", shift = 1).dueOn)
        assertEquals(LocalDate.parse("2026-12-07"), inter("2026-12-15", shift = -1).dueOn)
    }

    @Test
    fun dueDayBeforeClosingDayFallsInTheFollowingMonth() {
        // Nubank com fechamento dia 25 e vencimento dia 5.
        val statement = statementFor(25, 5, LocalDate.parse("2026-11-10"))

        assertEquals(LocalDate.parse("2026-11-25"), statement.closesOn)
        assertEquals(LocalDate.parse("2026-12-05"), statement.dueOn)
    }
}
