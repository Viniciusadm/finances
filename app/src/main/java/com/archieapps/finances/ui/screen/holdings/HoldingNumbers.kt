package com.archieapps.finances.ui.screen.holdings

import com.archieapps.finances.core.Money
import java.math.BigDecimal
import java.math.RoundingMode

/** Lê o que a pessoa digitou ("35,20", "1.234,56", "0.00012"); null se não for um número. */
fun parseDecimal(text: String): BigDecimal? {
    val raw = text.trim().filter { it.isDigit() || it == ',' || it == '.' }
    if (raw.isEmpty()) return null

    val normalized = if (raw.contains(',')) raw.replace(".", "").replace(',', '.') else raw

    return normalized.toBigDecimalOrNull()
}

fun isPositive(text: String): Boolean = (parseDecimal(text)?.signum() ?: 0) > 0

/** Quantidade × preço, em centavos, arredondado. */
fun valueCents(quantity: String, price: String): Long? {
    val q = parseDecimal(quantity) ?: return null
    val p = parseDecimal(price) ?: return null

    return q.multiply(p).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
}

/** Quantidade sem zeros sobrando: "10", "0,5", "0,00012". */
fun formatQuantity(value: String): String {
    val number = value.toBigDecimalOrNull() ?: parseDecimal(value) ?: return value
    val plain = number.stripTrailingZeros().toPlainString()

    return plain.replace('.', ',')
}

/** Preço em reais com no mínimo 2 casas ("35,20") e mais só quando precisa ("0,00012"). */
fun formatPrice(value: String): String {
    val number = value.toBigDecimalOrNull() ?: parseDecimal(value) ?: return value
    val stripped = number.stripTrailingZeros()
    val scale = stripped.scale().coerceAtLeast(2)
    val fixed = number.setScale(scale, RoundingMode.HALF_UP)
    val parts = fixed.toPlainString().split('.')
    val integer = Money.groupedDigits(parts[0])

    return if (parts.size > 1) "$integer,${parts[1]}" else integer
}

fun formatPercent(value: Double?): String {
    if (value == null) return "—"
    val sign = if (value > 0) "+" else ""

    return sign + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", value) + "%"
}

fun formatSigned(cents: Long): String {
    val sign = when {
        cents > 0 -> "+"
        cents < 0 -> "−"
        else -> ""
    }

    return "${sign}R$ ${Money.full(kotlin.math.abs(cents))}"
}
