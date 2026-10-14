package com.archieapps.finances.core.session

import kotlin.random.Random

object CodePad {
    const val SLOTS = 4

    fun shuffleKeypad(random: Random = Random.Default): List<String> =
        ('0'..'9').shuffled(random)
            .chunked(2)
            .map { pair -> pair.joinToString("") }

    fun assemble(chosen: List<String?>): String? {
        if (chosen.size != SLOTS || chosen.any { it == null || it.length != 2 }) {
            return null
        }

        return chosen.joinToString("")
    }

    fun label(option: String): String = option.toCharArray().joinToString(" ou ")
}
