package com.archieapps.finances.ui.screen.entries

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archieapps.finances.data.dto.EntryDto
import com.archieapps.finances.ui.components.Chip
import com.archieapps.finances.ui.components.EmptyState
import com.archieapps.finances.ui.components.ErrorBanner
import com.archieapps.finances.ui.components.Eyebrow
import com.archieapps.finances.ui.components.HairlineDivider
import com.archieapps.finances.ui.components.LedgerRow
import com.archieapps.finances.ui.components.LoadingRow
import com.archieapps.finances.ui.components.MoneySize
import com.archieapps.finances.ui.components.MoneyText
import com.archieapps.finances.ui.components.QuietAction
import com.archieapps.finances.ui.components.ScreenHeader
import com.archieapps.finances.ui.components.TextAction
import com.archieapps.finances.ui.nav.appViewModel
import com.archieapps.finances.ui.theme.Negative
import com.archieapps.finances.ui.theme.Positive
import com.archieapps.finances.ui.theme.TextFaint
import com.archieapps.finances.ui.theme.TextMuted
import com.archieapps.finances.ui.theme.TextStrong
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PtBr = Locale.forLanguageTag("pt-BR")
private val MonthFormat = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PtBr)
private val DayFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PtBr)

@Composable
fun EntriesScreen(onEdit: (Int) -> Unit) {
    val model = appViewModel { container -> EntriesViewModel(container.repository) }
    val state by model.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Lançamentos",
            action = { TextAction("Atualizar", model::refresh) }
        )

        if (!state.allTime) {
            MonthStepper(
                month = state.month,
                onPrevious = model::previousMonth,
                onNext = model::nextMonth
            )
        }

        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("Todo período", state.allTime, onClick = model::toggleAllTime)
            Chip("Gastos", state.kind == "expense", onClick = {
                model.setKind(if (state.kind == "expense") null else "expense")
            })
            Chip("Ganhos", state.kind == "income", onClick = {
                model.setKind(if (state.kind == "income") null else "income")
            })
            Chip("Transferências", state.kind == "transfer", onClick = {
                model.setKind(if (state.kind == "transfer") null else "transfer")
            })
            Chip("Só previstos", state.onlyUnpaid, onClick = model::toggleUnpaid)
        }

        Totals(expense = state.expenseCents, income = state.incomeCents)

        ErrorBanner(state.error)

        if (state.loading && state.entries.isEmpty()) {
            LoadingRow()
        }

        if (!state.loading && state.entries.isEmpty()) {
            EmptyState(
                headline = "Nada lançado aqui",
                hint = "Toque no + para lançar um gasto. Leva três toques: valor, valor, salvar."
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            state.grouped.forEach { (date, entries) ->
                item(key = "day-$date") {
                    DayHeader(date = date, totalCents = model.dayTotal(entries))
                }

                entries.forEach { entry ->
                    item(key = "entry-${entry.id}") {
                        EntryLine(entry = entry, onClick = { onEdit(entry.id) })
                        HairlineDivider(inset = 20)
                    }
                }
            }

            if (state.hasMore) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.loadingMore) {
                            LoadingRow()
                        } else {
                            QuietAction(label = "Carregar mais", onClick = model::loadMore)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun MonthStepper(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextAction("‹", onPrevious, color = TextMuted)
        Text(
            text = month.atDay(1).format(MonthFormat).replaceFirstChar { it.titlecase(PtBr) },
            style = MaterialTheme.typography.titleMedium,
            color = TextStrong,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
        )
        TextAction("›", onNext, color = TextMuted)
    }
}

@Composable
private fun Totals(expense: Long, income: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Eyebrow("Saiu")
            Spacer(Modifier.height(4.dp))
            MoneyText(cents = expense, size = MoneySize.Section, color = Negative, showSymbol = false)
        }
        Column {
            Eyebrow("Entrou")
            Spacer(Modifier.height(4.dp))
            MoneyText(cents = income, size = MoneySize.Section, color = Positive, showSymbol = false)
        }
    }
}

@Composable
private fun DayHeader(date: String, totalCents: Long) {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Eyebrow(
            text = parsed?.format(DayFormat) ?: date,
            modifier = Modifier.weight(1f)
        )
        MoneyText(
            cents = totalCents,
            size = MoneySize.Inline,
            showSymbol = false,
            color = if (totalCents >= 0) Positive else TextFaint
        )
    }
}

@Composable
private fun EntryLine(entry: EntryDto, onClick: () -> Unit) {
    LedgerRow(
        title = entry.description,
        subtitle = subtitleFor(entry),
        markerHex = entry.color,
        dimmed = !entry.paid,
        onClick = onClick,
        trailing = {
            MoneyText(
                cents = entry.amountCents,
                size = MoneySize.Row,
                showSymbol = false,
                color = when {
                    !entry.paid -> TextFaint
                    entry.kind == "income" -> Positive
                    entry.kind == "transfer" -> TextMuted
                    else -> TextStrong
                }
            )
        }
    )
}

private fun subtitleFor(entry: EntryDto): String {
    val where = when {
        entry.kind == "transfer" -> listOfNotNull(entry.accountName, entry.counterAccountName).joinToString(" → ")
        entry.cardName != null -> entry.cardName
        entry.accountName != null -> entry.accountName
        else -> null
    }

    val parts = listOfNotNull(
        entry.categoryName?.takeIf { it != entry.description },
        where,
        entry.personName,
        if (entry.paid) null else "previsto"
    )

    return parts.joinToString(" · ")
}
