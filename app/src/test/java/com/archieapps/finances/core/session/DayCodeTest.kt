package com.archieapps.finances.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DayCodeTest {

    private fun clockAt(instant: String): Clock =
        Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)

    @Test
    fun `codigo guardado hoje vale hoje`() {
        val clock = clockAt("2026-08-27T15:00:00Z")

        assertTrue(DayCode.isValidFor("2026-08-27", clock))
    }

    @Test
    fun `codigo guardado ontem nao vale mais`() {
        val clock = clockAt("2026-08-27T15:00:00Z")

        assertFalse(DayCode.isValidFor("2026-08-26", clock))
    }

    @Test
    fun `codigo de amanha tambem nao vale`() {
        val clock = clockAt("2026-08-27T15:00:00Z")

        assertFalse(DayCode.isValidFor("2026-08-28", clock))
    }

    @Test
    fun `validade ausente ou vazia nao vale`() {
        val clock = clockAt("2026-08-27T15:00:00Z")

        assertFalse(DayCode.isValidFor(null, clock))
        assertFalse(DayCode.isValidFor("", clock))
        assertFalse(DayCode.isValidFor("   ", clock))
    }

    @Test
    fun `o dia segue o fuso do servidor, nao o UTC`() {
        // 02:00 UTC do dia 28 ainda e dia 27 em Sao Paulo (UTC-3)
        val clock = clockAt("2026-08-28T02:00:00Z")

        assertEquals("2026-08-27", DayCode.serverToday(clock).toString())
        assertTrue(DayCode.isValidFor("2026-08-27", clock))
        assertFalse(DayCode.isValidFor("2026-08-28", clock))
    }

    @Test
    fun `a virada em Sao Paulo invalida o codigo do dia anterior`() {
        val antes = clockAt("2026-08-28T02:59:00Z")
        val depois = clockAt("2026-08-28T03:01:00Z")

        assertTrue("23h59 de SP ainda vale", DayCode.isValidFor("2026-08-27", antes))
        assertFalse("00h01 de SP nao vale mais", DayCode.isValidFor("2026-08-27", depois))
    }

    @Test
    fun `codigo bem formado tem oito caracteres sem espaco`() {
        assertTrue(DayCode.isWellFormed("59241867"))
        assertFalse(DayCode.isWellFormed("5924186"))
        assertFalse(DayCode.isWellFormed("592418677"))
        assertFalse(DayCode.isWellFormed("5924 867"))
    }
}
