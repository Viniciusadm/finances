package com.archieapps.finances.ui.screen.composer

import java.time.LocalDate
import java.time.YearMonth

/** A fatura de um cartão, no mesmo cálculo da API (`StatementWindow`): fechamento inclusivo. */
data class StatementRef(val startsOn: LocalDate, val closesOn: LocalDate, val dueOn: LocalDate) {
    val reference: YearMonth get() = YearMonth.from(dueOn)

    /** "dez/2026 · vence 07/12" */
    fun label(): String {
        val month = Months[dueOn.monthValue - 1]
        val day = dueOn.dayOfMonth.toString().padStart(2, '0')
        val monthNumber = dueOn.monthValue.toString().padStart(2, '0')

        return "$month/${dueOn.year} · vence $day/$monthNumber"
    }

    private companion object {
        val Months = listOf("jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez")
    }
}

private fun dayIn(month: LocalDate, day: Int): LocalDate {
    val first = month.withDayOfMonth(1)

    return first.withDayOfMonth(minOf(day, first.lengthOfMonth()))
}

private fun build(date: LocalDate, closingDay: Int, dueDay: Int): StatementRef {
    val closeThisMonth = dayIn(date, closingDay)
    val closesOn = if (date <= closeThisMonth) closeThisMonth else dayIn(date.plusMonths(1), closingDay)
    val startsOn = dayIn(closesOn.minusMonths(1), closingDay).plusDays(1)
    val dueMonth = if (dueDay > closingDay) closesOn else closesOn.plusMonths(1)

    return StatementRef(startsOn, closesOn, dayIn(dueMonth, dueDay))
}

/**
 * Fatura em que uma compra cai. `shift` = 1 é a fatura seguinte e -1 a anterior,
 * para o banco que processa a compra fora do dia.
 */
fun statementFor(closingDay: Int, dueDay: Int, date: LocalDate, shift: Int = 0): StatementRef {
    val closing = closingDay.coerceIn(1, 31)
    val due = dueDay.coerceIn(1, 31)
    var window = build(date, closing, due)

    repeat(kotlin.math.abs(shift)) {
        window = if (shift > 0) build(window.closesOn.plusDays(1), closing, due) else build(window.startsOn.minusDays(1), closing, due)
    }

    return window
}
