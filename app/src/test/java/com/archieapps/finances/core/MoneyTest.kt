package com.archieapps.finances.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun `reais e centavos digitados viram centavos`() {
        assertEquals(2000L, Money.fromTyped("20", ""))
        assertEquals(2050L, Money.fromTyped("20", "5"))
        assertEquals(2055L, Money.fromTyped("20", "55"))
        assertEquals(0L, Money.fromTyped("", ""))
        assertEquals(90L, Money.fromTyped("", "90"))
    }

    @Test
    fun `formata no padrao brasileiro`() {
        assertEquals("1.234,56", Money.full(123456))
        assertEquals("0,05", Money.full(5))
        assertEquals("-45,10", Money.full(-4510))
    }

    @Test
    fun `separa inteiro e centavos para o MoneyText`() {
        assertEquals("1.234", Money.integerPart(123456))
        assertEquals("56", Money.centsPart(123456))
        assertEquals("07", Money.centsPart(107))
    }
}
