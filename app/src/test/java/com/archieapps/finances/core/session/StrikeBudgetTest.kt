package com.archieapps.finances.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrikeBudgetTest {

    @Test
    fun `comeca podendo tentar`() {
        assertTrue(StrikeBudget().canAttempt)
        assertEquals(StrikeBudget.LOCKOUT, StrikeBudget().remainingShown)
    }

    @Test
    fun `duas recusas locais travam a tentativa`() {
        val budget = StrikeBudget().afterRejection(null).afterRejection(null)

        assertTrue(budget.hardStopped)
        assertFalse(budget.canAttempt)
    }

    @Test
    fun `usa o que o servidor reporta quando e pior que a contagem local`() {
        val budget = StrikeBudget().afterRejection(1)

        assertEquals(3, budget.serverBlocked)
        assertEquals(2, budget.remainingShown)
    }

    @Test
    fun `sucesso zera o placar`() {
        val budget = StrikeBudget().afterRejection(null).afterSuccess()

        assertEquals(StrikeBudget(), budget)
        assertTrue(budget.canAttempt)
    }
}
