package com.archieapps.finances.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.absoluteValue

object Money {
    private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("pt-BR")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val grouped = DecimalFormat("#,##0", symbols)

    fun integerPart(cents: Long): String = grouped.format(cents.absoluteValue / 100)

    fun centsPart(cents: Long): String = (cents.absoluteValue % 100).toString().padStart(2, '0')

    fun sign(cents: Long): String = if (cents < 0) "-" else ""

    fun full(cents: Long): String = "${sign(cents)}${integerPart(cents)},${centsPart(cents)}"

    fun fromTypedDigits(digits: String): Long = digits.take(12).toLongOrNull() ?: 0L

    fun fromTyped(integerDigits: String, centsDigits: String): Long {
        val units = integerDigits.take(9).toLongOrNull() ?: 0L
        val cents = centsDigits.padEnd(2, '0').take(2).toLongOrNull() ?: 0L
        return units * 100 + cents
    }

    fun groupedDigits(integerDigits: String): String {
        val units = integerDigits.toLongOrNull() ?: return "0"
        return grouped.format(units)
    }
}
