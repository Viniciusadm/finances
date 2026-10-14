package com.archieapps.finances.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class CodePadTest {

    @Test
    fun `o teclado cobre os dez digitos em cinco pares`() {
        val keypad = CodePad.shuffleKeypad(Random(7))

        assertEquals(5, keypad.size)
        assertEquals(
            ('0'..'9').toSet(),
            keypad.joinToString("").toSet()
        )
    }

    @Test
    fun `montar exige as quatro escolhas`() {
        assertNull(CodePad.assemble(listOf(null, null, null, null)))
        assertNull(CodePad.assemble(listOf("59", "24", "18", null)))
        assertNull(CodePad.assemble(listOf("59", "24", "18")))
        assertEquals("59241867", CodePad.assemble(listOf("59", "24", "18", "67")))
    }

    @Test
    fun `o rotulo mostra as duas opcoes da tecla`() {
        assertEquals("5 ou 9", CodePad.label("59"))
    }
}
